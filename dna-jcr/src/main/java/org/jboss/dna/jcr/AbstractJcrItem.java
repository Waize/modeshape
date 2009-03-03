/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

/**
 * @author jverhaeg
 */
abstract class AbstractJcrItem implements Item {

    protected final String getPath( String absolutePath,
                                    String relativePath ) {
        assert absolutePath != null;
        assert absolutePath.length() > 0;
        assert relativePath != null;
        if (absolutePath.charAt(absolutePath.length() - 1) == '/') {
            return absolutePath + relativePath;
        }
        return absolutePath + '/' + relativePath;
    }

    /**
     * {@inheritDoc}
     * 
     * @return <code>false</code>
     * @see javax.jcr.Item#isModified()
     */
    public final boolean isModified() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @return <code>false</code>
     * @see javax.jcr.Item#isNew()
     */
    public final boolean isNew() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#isSame(javax.jcr.Item)
     */
    public boolean isSame( Item otherItem ) throws RepositoryException {
        return (getSession().getWorkspace() == otherItem.getSession().getWorkspace());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getAncestor(int)
     */
    public final Item getAncestor( int depth ) throws RepositoryException {
        if (depth < 0) {
            throw new ItemNotFoundException(JcrI18n.noNegativeDepth.text(depth));
        }

        /*
         * depth argument is absolute depth from root of content graph, not relative depth from current node.
         * That is, if current node is at path /foo/bar/baz, getAncestor(1) returns node at /foo, getAncestor(2)
         * returns node at /foo/bar, getAncestor(3) returns node at /foo/bar/baz, getAncestor(0) returns root node,
         * and any other argument results in ItemNotFoundException.
         * Next statement converts depth parameter from a relative depth to an absolute depth.
         */
        depth = getDepth() - depth;

        if (depth < 0) {
            throw new ItemNotFoundException(JcrI18n.tooDeep.text(depth));
        }

        Item ancestor = this;
        while (--depth >= 0) {
            ancestor = ancestor.getParent();
        }
        return ancestor;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getDepth()
     */
    public int getDepth() throws RepositoryException {
        return getParent().getDepth() + 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Item#refresh(boolean)
     */
    public void refresh( boolean keepChanges ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Item#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Item#save()
     */
    public void save() {
        throw new UnsupportedOperationException();
    }
}
