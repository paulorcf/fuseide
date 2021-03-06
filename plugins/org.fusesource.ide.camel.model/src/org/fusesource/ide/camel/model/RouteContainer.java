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

package org.fusesource.ide.camel.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.draw2d.geometry.Rectangle;
import org.fusesource.camel.tooling.util.ValidationHandler;
import org.fusesource.camel.tooling.util.XmlModel;
import org.fusesource.ide.camel.model.generated.Route;
import org.fusesource.ide.commons.util.Objects;



/**
 * @author lhein
 */
public class RouteContainer extends AbstractNode {

	protected static final transient Log LOG = LogFactory.getLog(RouteContainer.class);

	/** Property ID to use when a child is added to this diagram. */
	public static final String CHILD_ADDED_PROP = "Route.ChildAdded";

	/** Property ID to use when a child is removed from this diagram. */
	public static final String CHILD_REMOVED_PROP = "Route.ChildRemoved";

	private List<AbstractNode> children = new ArrayList<AbstractNode>();

	private Map<String,String> beans = new HashMap<String,String>();

	private Set<String> camelContextEndpointUris = new TreeSet<String>();

	private boolean autoLayout;

	private XmlModel model;

	private boolean failedToParseXml;

	public RouteContainer() {
	}

	public RouteContainer(RouteContainer parent) {
		super(parent);
	}

	@Override
	public void clearResources() {
		super.clearResources();
		List<AbstractNode> children = getChildren();
		for (AbstractNode node : children) {
			node.clearResources();
		}
	}
	public String[] getEndpointUris() {
		// lets iterate through each route and for each route iterate through each node finding the URIs used...
		Set<String> uris = new TreeSet<String>();
		appendEndpointUris(uris);
		uris.addAll(getCamelContextEndpointUris());
		return uris.toArray(new String[uris.size()]);
	}

	@Override
	public void appendEndpointUris(Set<String> uris) {
		List<AbstractNode> outputs = getChildren();
		for (AbstractNode node : outputs) {
			node.appendEndpointUris(uris);
		}
	}


	public Set<String> getCamelContextEndpointUris() {
		return camelContextEndpointUris;
	}

	public void setCamelContextEndpointUris(Set<String> camelContextEndpointUris) {
		this.camelContextEndpointUris = camelContextEndpointUris;
	}

	public Map<String, String> getBeans() {
		return beans;
	}

	public void setBeans(Map<String, String> beans) {
		this.beans = beans;
	}

	/**
	 * Adds the list of route definitions after loading the routes from a file or runtime
	 */
	public void addRoutes(List<RouteDefinition> routes) {
		for (RouteDefinition routeDefinition : routes) {
			RouteSupport route = new Route(routeDefinition, this);
			addChild(route);
		}
	}

	/**
	 * Add a child to this diagram.
	 * @param node a non-null child instance
	 * @return true, if the child was added, false otherwise
	 */
	public void addChild(AbstractNode node) {
		addChild(node, null);
	}

	/**
	 * adds a child behind another child
	 * 
	 * @param child
	 * @param after
	 * @return
	 */
	public void addChild(AbstractNode child, AbstractNode after) {
		int idx = this.children.indexOf(after);
		addChild(child, idx != -1 ? (idx+1) : idx);
	}

	/**
	 * adds the child at the given index
	 * 
	 * @param child
	 * @param index
	 */
	public void addChild(AbstractNode child, int index) {
		if (this.children.contains(child)) {
			if (index >= 0 && index < children.size()) {
				Activator.getLogger().warning("WARN: adding child at index: " + index + " when it was already there in " + this + " children: " + children);
			}
			return;
		}
		if (index < 0 || index > this.children.size()-1) {
			this.children.add(child);
		} else {
			this.children.add(index, child);
		}
		if (child.getParent() == null) {
			child.setParent(this);
		}
		/*
 		// assign to same Route...
		RouteContainer parent = getParent();
		if (parent instanceof Route && !(child instanceof RouteContainer)) {
			child.setParent(parent);
		}
		 */
		fireChildAdded(CHILD_ADDED_PROP, child, new Integer(index));
	}

	/**
	 * Adds the given children
	 */
	public void addChildren(AbstractNode... nodes) {
		for (int i = 0; i < nodes.length; i++) {
			addChild(nodes[i]);
		}
	}

	/** Return a List of Shapes in this diagram.  The returned List should not be modified. */
	@Override
	public List<AbstractNode> getChildren() {
		return new ArrayList<AbstractNode>(this.children);
	}


	/**
	 * Returns all the nodes which are not used as a target
	 */
	public List<AbstractNode> getSourceNodes() {
		ArrayList<AbstractNode> answer = new ArrayList<AbstractNode>();
		for (AbstractNode child : children) {
			if (child.getTargetConnections().size() == 0) {
				answer.add(child);
			}
		}
		return answer;
	}

	/**
	 * Remove a shape from this diagram.
	 * @param node a non-null shape instance;
	 * @return true, if the shape was removed, false otherwise
	 */
	public boolean removeChild(AbstractNode node) {
		if (node != null && this.children.remove(node)) {
			node.setParent(null);
			firePropertyChange(CHILD_REMOVED_PROP, null, node);
			return true;
		}
		return false;
	}


	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.AbstractNode#createCamelDefinition()
	 */
	@Override
	public ProcessorDefinition createCamelDefinition() {
		return null;
	}

	/**
	 * Lets recreate the model clearing any associated EMF data such as links to diagram elements etc
	 */
	public RouteContainer recreateModel() {
		List<RouteDefinition> definitions = createRouteDefinitions();

		// lets clone everything in our model apart from EMF stuff and children
		RouteContainer answer = new RouteContainer();
		answer.beans = beans;
		answer.camelContextEndpointUris = camelContextEndpointUris;
		answer.autoLayout = autoLayout;
		answer.model = model;
		answer.addRoutes(definitions);
		return answer;
	}

	/**
	 * Returns the list of route definitions for this container
	 */
	public List<RouteDefinition> createRouteDefinitions() {
		List<RouteDefinition> answer = new ArrayList<RouteDefinition>();
		List<AbstractNode> list = getChildren();
		for (AbstractNode node : list) {
			if (node instanceof RouteSupport) {
				RouteSupport route = (RouteSupport) node;
				RouteDefinition routeDef = route.createRouteDefinition();
				answer.add(routeDef);
			} else {
				LOG.warn("Bad node in RouteContainer is ignored: " + node);
			}
		}
		return answer;
	}
	/**
	 * creates debug info
	 * @return
	 */
	public String getDebugInfo() {
		StringBuffer sb = new StringBuffer();

		for (AbstractNode r : this.children) {
			if (r instanceof RouteSupport) {
				sb.append(((RouteSupport)r).createCamelDefinition().toString());
				sb.append('\n');
				sb.append('\n');
			}
		}
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see org.fusesource.ide.camel.model.AbstractNode#layout()
	 */
	@Override
	public void layout() {
		// skip layout for new empty containers/routes otherwise it will be placed always
		// at location 0,0 instead of where the user clicked
		if (autoLayout || (getParent() != null && getParent().isAutoLayout()) ||
				(getChildren().size() == 0 && getLayout().x != 0 && getLayout().y != 0)) {
			return;
		}

		int x_min = 0, y_min = 0, maxWidth = 100, maxHeight = 50;

		boolean first = true;
		for (AbstractNode
				node : getChildren()) {
			node.layout();
			Rectangle r = node.getLayout();
			if (first) {
				first = false;
				x_min = r.x;
				y_min = r.y;
				maxWidth = r.x + r.width;
				maxHeight = r.y + r.height;
			} else {
				x_min = Math.min(x_min, r.x);
				y_min = Math.min(y_min, r.y);
				maxWidth = Math.max(maxWidth, r.x + r.width);
				maxHeight = Math.max(maxHeight, r.y + r.height);
			}
		}

		// finally make it a bit bigger
		x_min = Math.max(0, x_min - 5);
		y_min = Math.max(0, y_min - 5);
		maxWidth += 100;
		maxHeight += 100;

		setLayout(new Rectangle(x_min, y_min, maxWidth, maxHeight));
	}


	public Set<AbstractNode> getDescendents() {
		// lets preserve order of insertion to ensure we get consistent
		// layouts of graphs - changing the order of the tree changes the layout algorithm results!
		Set<AbstractNode> answer = new LinkedHashSet<AbstractNode>();
		appendDescendents(answer);
		return answer;
	}

	@Override
	protected void appendDescendents(Set<AbstractNode> answer) {
		List<AbstractNode> list = getChildren();
		for (AbstractNode node : list) {
			node.appendDescendents(answer);
		}
		answer.add(this);
	}

	public Set<Endpoint> getEndpoints() {
		Set<AbstractNode> descendents = getDescendents();
		Set<Endpoint> answer = new LinkedHashSet<Endpoint>();
		for (AbstractNode node : descendents) {
			if (node instanceof Endpoint) {
				answer.add((Endpoint) node);
			}
		}
		return answer;
	}

	public boolean isAutoLayout() {
		return autoLayout;
	}

	public void setAutoLayout(boolean autoLayout) {
		this.autoLayout = autoLayout;
	}

	/**
	 * Looks up the node by its ID
	 */
	public AbstractNode getNode(String id) {
		Set<AbstractNode> set = getDescendents();
		for (AbstractNode abstractNode : set) {
			if (Objects.equal(id, abstractNode.getId())) {
				return abstractNode;
			}
		}
		return null;
	}

	public XmlModel getModel() {
		return model;
	}

	public void setModel(XmlModel model) {
		this.model = model;
	}

	public ValidationHandler validate() {
		if (model != null) {
			return model.validate();
		} else {
			// typically when we've not been able to parse the XML yet...
			return new ValidationHandler();
		}
	}


	public boolean isFailedToParseXml() {
		return failedToParseXml;
	}

	public void setFailedToParseXml(boolean failedToParseXml) {
		this.failedToParseXml = failedToParseXml;
	}

}
