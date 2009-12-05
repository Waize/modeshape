package org.jboss.dna.connector.store.jpa.model.simple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityTransaction;
import org.jboss.dna.connector.store.jpa.JpaConnectorI18n;
import org.jboss.dna.connector.store.jpa.JpaSource;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.map.MapNode;
import org.jboss.dna.graph.connector.map.MapRequestProcessor;
import org.jboss.dna.graph.observe.Observer;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.request.CloneWorkspaceRequest;
import org.jboss.dna.graph.request.CreateWorkspaceRequest;
import org.jboss.dna.graph.request.InvalidRequestException;
import org.jboss.dna.graph.request.ReadBranchRequest;
import org.jboss.dna.graph.request.processor.RequestProcessor;
import com.google.common.collect.LinkedListMultimap;

/**
 * Extension of the {@link MapRequestProcessor} that provides a {@link #process(ReadBranchRequest)} implementation optimized for
 * the {@link SimpleModel simple JPA model}. This class also provides some JPA-specific functionality for the ability to control
 * whether {@link JpaSource#isCreatingWorkspacesAllowed() creating workspaces is allowed}.
 */
public class SimpleRequestProcessor extends MapRequestProcessor {

    private final SimpleJpaRepository repository;
    private final PathFactory pathFactory;

    public SimpleRequestProcessor( ExecutionContext context,
                                   SimpleJpaRepository repository,
                                   Observer observer ) {
        super(context, repository, observer);

        this.repository = repository;
        this.pathFactory = context.getValueFactories().getPathFactory();
    }

    @Override
    public void close() {
        EntityTransaction tx = repository.entityManager().getTransaction(); 
        if (tx != null) {
            tx.commit();
        }
        super.close();
    }

    /**
     * Override the {@link RequestProcessor#process(ReadBranchRequest) default handling} for a read branch request to optimize the
     * queries involved.
     * 
     * @param request the request to read
     */
    @Override
    public void process( ReadBranchRequest request ) {
        SimpleJpaRepository.Workspace workspace = (SimpleJpaRepository.Workspace)getWorkspace(request, request.inWorkspace());

        int maximumDepth = request.maximumDepth();
        List<MapNode> branch = workspace.getBranch(request.at(), maximumDepth);

        if (!branch.isEmpty()) {
            Map<UUID, LocationWithDepth> locations = new HashMap<UUID, LocationWithDepth>(branch.size());

            /*
             * Add the first (root) node to the request
             */
            MapNode root = branch.get(0);
            Location rootLocation = getActualLocation(request.at(), root);
            request.setActualLocationOfNode(rootLocation);
            locations.put(root.getUuid(), new LocationWithDepth(rootLocation, 0));

            /*
             * The obvious thing to do here would be to call root.getChildren(), but that would
             * result in the JPA implementation running an extra query to load the collection of
             * children for the entity even though we've already loaded all of the children 
             * with the call to workspace.getBranch(...) earlier.  
             * 
             * We'll build the list of children ourselves knowing that all children are in the result set.
             * 
             * The concrete type is used in the variable declaration instead of the relevant interface
             * (Multimap<UUID, Location>) because we need to cast the result of a .get(UUID) operation
             * to a List<Location> below and the interface only guarantees a Collection<Location>.
             */
            LinkedListMultimap<UUID, Location> childrenByParentUuid = LinkedListMultimap.create();

            /*
             * We don't want to process the root node (the first node) in this loop
             * as this would cause us to unnecessarily load the root node's parent node.
             */
            for (int i = 1; i < branch.size(); i++) {
                MapNode node = branch.get(i);
                UUID parentUuid = node.getParent().getUuid();

                LocationWithDepth parentLocation = locations.get(parentUuid);
                Location nodeLocation = locationFor(parentLocation.getLocation(), node);
                locations.put(node.getUuid(), new LocationWithDepth(nodeLocation, parentLocation.getDepth() + 1));

                childrenByParentUuid.put(parentUuid, locationFor(locations.get(parentUuid).getLocation(), node));
            }

            request.setChildren(rootLocation, childrenByParentUuid.get(root.getUuid()));
            request.setProperties(rootLocation, root.getProperties().values());

            /*
             * Process the subsequent nodes
             */
            for (int i = 1; i < branch.size(); i++) {
                MapNode node = branch.get(i);

                UUID nodeUuid = node.getUuid();
                LocationWithDepth nodeLocation = locations.get(nodeUuid);
                if (nodeLocation.getDepth() < maximumDepth) {
                    request.setChildren(nodeLocation.getLocation(), childrenByParentUuid.get(nodeUuid));
                    request.setProperties(nodeLocation.getLocation(), node.getProperties().values());
                }
            }
        }

        setCacheableInfo(request);
    }

    private Location locationFor( Location parentLocation,
                                  MapNode node ) {
        return Location.create(pathFactory.create(parentLocation.getPath(), node.getName()), node.getUuid());
    }

    @Override
    public void process( CreateWorkspaceRequest request ) {
        if (!repository.creatingWorkspacesAllowed()) {
            String msg = JpaConnectorI18n.unableToCreateWorkspaces.text(getSourceName());
            request.setError(new InvalidRequestException(msg));
            return;
        }

        super.process(request);
    }

    @Override
    public void process( CloneWorkspaceRequest request ) {
        if (!repository.creatingWorkspacesAllowed()) {
            String msg = JpaConnectorI18n.unableToCreateWorkspaces.text(getSourceName());
            request.setError(new InvalidRequestException(msg));
            return;
        }

        super.process(request);
    }

}
