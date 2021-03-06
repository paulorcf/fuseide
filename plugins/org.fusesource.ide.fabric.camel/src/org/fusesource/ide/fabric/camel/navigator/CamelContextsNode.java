/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.fusesource.ide.fabric.camel.navigator;

import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.fusesource.fabric.camel.facade.CamelFacade;
import org.fusesource.fabric.camel.facade.mbean.CamelContextMBean;
import org.fusesource.ide.commons.tree.Node;
import org.fusesource.ide.commons.tree.RefreshableCollectionNode;
import org.fusesource.ide.commons.ui.ImageProvider;
import org.fusesource.ide.fabric.FabricPlugin;
import org.fusesource.ide.fabric.camel.FabricCamelPlugin;


public class CamelContextsNode extends RefreshableCollectionNode implements ImageProvider {
	private final CamelFacade facade;

	public CamelContextsNode(Node parent, CamelFacade facade) {
		super(parent);
		this.facade = facade;
	}

	@Override
	public String toString() {
		return "Camel";
	}

	public CamelFacade getFacade() {
		return facade;
	}

	@Override
	protected void loadChildren() {
		try {
			List<CamelContextMBean> camelContexts = facade.getCamelContexts();
			if (camelContexts != null) {
				for (CamelContextMBean camelContextMBean : camelContexts) {
					CamelContextNode child = new CamelContextNode(this, facade, camelContextMBean);
					addChild(child);
				}
			}
		} catch (Exception e) {
			FabricCamelPlugin.getLogger().warning("Failed to connect to JMX: " + e, e);
		}
	}

	public void addChildren(CamelFacade facade) throws Exception {
	}


	@Override
	public Image getImage() {
		return FabricPlugin.getDefault().getImage("camel.png");
	}

}
