/*
 *  Licensed to Peter Karich under one or more contributor license 
 *  agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  Peter Karich licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except 
 *  in compliance with the License. You may obtain a copy of the 
 *  License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.reader;

import com.graphhopper.routing.util.AcceptWay;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphStorage;
import com.graphhopper.util.BitUtil;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.PointList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Peter Karich
 */
public abstract class OSMReaderHelper {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    protected long zeroCounter = 0;
    private long edgeCount;
    protected final Graph g;
    protected final long expectedNodes;
    private DistanceCalc callback = new DistanceCalc();
    private AcceptWay acceptWay;
    protected TLongArrayList wayNodes = new TLongArrayList(10);
    private Map<String, Object> osmProperties = new HashMap<String, Object>();
    private Map<String, Object> outProperties = new HashMap<String, Object>();

    public OSMReaderHelper(Graph g, long expectedNodes) {
        this.g = g;
        this.expectedNodes = expectedNodes;
    }

    public OSMReaderHelper acceptWay(AcceptWay acceptWay) {
        this.acceptWay = acceptWay;
        return this;
    }

    public AcceptWay acceptWay() {
        return acceptWay;
    }

    public void callback(DistanceCalc callback) {
        this.callback = callback;
    }

    public long expectedNodes() {
        return expectedNodes;
    }

    public long edgeCount() {
        return edgeCount;
    }

    public void preProcess(InputStream osmXml) {
    }

    public abstract boolean addNode(long osmId, double lat, double lon);

    public abstract int addEdge(TLongList nodes, int flags, int name);

    int addEdge(int fromIndex, int toIndex, PointList pointList, int flags, int name) {
        if (fromIndex < 0 || toIndex < 0)
            throw new AssertionError("to or from index is invalid for this edge "
                    + fromIndex + "->" + toIndex + ", points:" + pointList);

        double towerNodeDistance = 0;
        double prevLat = pointList.latitude(0);
        double prevLon = pointList.longitude(0);
        double lat;
        double lon;
        PointList pillarNodes = new PointList(pointList.size() - 2);
        int nodes = pointList.size();
        for (int i = 1; i < nodes; i++) {
            lat = pointList.latitude(i);
            lon = pointList.longitude(i);
            towerNodeDistance += callback.calcDist(prevLat, prevLon, lat, lon);
            prevLat = lat;
            prevLon = lon;
            if (nodes > 2 && i < nodes - 1)
                pillarNodes.add(lat, lon);
        }
        if (towerNodeDistance == 0) {
            // As investigation shows often two paths should have crossed via one identical point 
            // but end up in two very close points.
            zeroCounter++;
            towerNodeDistance = 0.0001;
        }

        EdgeIterator iter = g.edge(fromIndex, toIndex, towerNodeDistance, flags, name);
        if (nodes > 2)
            iter.wayGeometry(pillarNodes);
        return nodes;
    }

    String getInfo() {
        return "Found " + zeroCounter + " zero distances.";
    }

    String getStorageInfo(GraphStorage storage) {
        return storage.getClass().getSimpleName() + "|" + storage.directory().getClass().getSimpleName()
                + "|" + storage.version();
    }

    void cleanup() {
    }

    void startWayProcessing() {
    }

    public void processWay(XMLStreamReader sReader) throws XMLStreamException {
        boolean valid = parseWay(sReader);
        if (valid) {
            int flags = acceptWay.toFlags(outProperties);
            int successfullAdded = addEdge(wayNodes, flags, 0);
            edgeCount += successfullAdded;
        }
    }

    /**
     * wayNodes will be filled with participating node ids. outProperties will
     * be filled with way information after calling this method.
     *
     * @return true the current xml entry is a way entry and has nodes
     */
    boolean parseWay(XMLStreamReader sReader) throws XMLStreamException {
        wayNodes.clear();
        osmProperties.clear();
        outProperties.clear();
        for (int tmpE = sReader.nextTag(); tmpE != XMLStreamConstants.END_ELEMENT;
                tmpE = sReader.nextTag()) {
            if (tmpE == XMLStreamConstants.START_ELEMENT) {
                if ("nd".equals(sReader.getLocalName())) {
                    String ref = sReader.getAttributeValue(null, "ref");
                    try {
                        wayNodes.add(Long.parseLong(ref));
                    } catch (Exception ex) {
                        logger.error("cannot get ref from way. ref:" + ref, ex);
                    }
                } else if ("tag".equals(sReader.getLocalName())) {
                    String tagKey = sReader.getAttributeValue(null, "k");
                    if (tagKey != null && !tagKey.isEmpty()) {
                        String tagValue = sReader.getAttributeValue(null, "v");
                        osmProperties.put(tagKey, tagValue);
                    }
                }
                sReader.next();
            }
        }

        boolean isWay = acceptWay.handleTags(outProperties, osmProperties, wayNodes);
        boolean hasNodes = wayNodes.size() > 1;
        return isWay && hasNodes;
    }
}
