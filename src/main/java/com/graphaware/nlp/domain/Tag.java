/*
 * Copyright (c) 2013-2016 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.graphaware.nlp.domain;

import static com.graphaware.nlp.domain.Labels.Tag;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

public class Tag implements Persistable {

    private int multiplicity = 1;
    private final String lemma;
    private String pos;
    private String ne;
    private Collection<TagParentRelation> parents;

    public Tag(String lemma) {
        this.lemma = lemma;
    }

    public String getLemma() {
        return lemma;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public void setNe(String ne) {
        this.ne = ne;
    }

    public int getMultiplicity() {
        return multiplicity;
    }

    public void incMultiplicity() {
        multiplicity++;
    }

    public String getPos() {
        return pos;
    }

    public String getNe() {
        return ne;
    }

    public void addParent(String rel, Tag storedTag) {
        if (parents == null) {
            parents = new HashSet<>();
        }
        parents.add(new TagParentRelation(storedTag, rel));
    }

    @Override
    public Node storeOnGraph(GraphDatabaseService database) {
        Node tagNode = getOrCreate(database);
        if (parents != null) {
            parents.stream().forEach((tagRelationship) -> {
                Node parentTagNode = tagRelationship.getParent().storeOnGraph(database);
                Map<String, Object> params = new HashMap<>();
                params.put("type", tagRelationship.getRelation());
                params.put("sourceId", tagNode.getId());
                params.put("destId", parentTagNode.getId());
                database.execute("MATCH (source:Tag), (destination:Tag)\n"
                        + "WHERE id(source) = {sourceId} and id(destination) = {destId}\n"
                        + "MERGE (source)-[:IS_RELATED_TO {type: {type}}]->(destination)" , params);
            });
        }
        return tagNode;
    }

    private Node getOrCreate(GraphDatabaseService database) {
        Node tagNode = database.findNode(Tag, "value", lemma);
        if (tagNode != null) {
            return tagNode;
        }
        tagNode = database.createNode(Tag);
        tagNode.setProperty("value", lemma);
        if (ne != null) {
            tagNode.setProperty("ne", ne);
        }
        if (pos != null) {
            tagNode.setProperty("pos", pos);
        }
        return tagNode;
    }

    public static Tag createTag(Node tagNode) {
        checkNodeIsATag(tagNode);
        Tag tag = new Tag((String) tagNode.getProperty("value"));
        return tag;
    }

    private static void checkNodeIsATag(Node tagNode) {
        Map<String, Object> allProperties = tagNode.getAllProperties();
        assert (tagNode.hasLabel(Tag));
        assert (allProperties.containsKey("value"));
    }
}