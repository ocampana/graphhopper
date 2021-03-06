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
package com.graphhopper.storage.index;

import com.graphhopper.geohash.KeyAlgo;
import com.graphhopper.geohash.SpatialKeyAlgo;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.Graph;
import com.graphhopper.trees.CoordResolver;
import com.graphhopper.trees.QuadTree;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistancePlaneProjection;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PointList;
import com.graphhopper.util.RawEdgeIterator;
import com.graphhopper.util.shapes.CoordTrig;
import com.graphhopper.util.shapes.GHPlace;
import com.graphhopper.util.shapes.Shape;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.util.Collection;

/**
 * This implementation implements a spare n-tree to get edgeIds from GPS
 * location. This will replace Location2IDQuadtree.
 *
 * @author Peter Karich
 */
public class Location2NodesNtree implements Location2NodesIndex, Location2IDIndex, QuadTree<Number> {

    private static final EdgeFilter ALL_EDGES = new EdgeFilter() {
        @Override public boolean accept(EdgeIterator iter) {
            return true;
        }
    };
    private DistanceCalc dist = new DistancePlaneProjection();
    private DataAccess dataAccess;
    private Graph g;
    /**
     * With maximum depth you control precision versus memory usage. The higher
     * the more memory wasted due to references but more precise value selection
     * can happen.
     */
    private int maxDepth;
    private int n;
    private int maxEntriesPerLeaf = 10;
    private long size;
    private CoordResolver<Integer> resolver = new ArrayCoordResolver();
    private KeyAlgo keyAlgo;

    private static class ArrayCoordResolver implements CoordResolver<Integer> {

        TIntList lats = new TIntArrayList();
        TIntList lons = new TIntArrayList();

        public void add(double lat, double lon, Number value) {
            int index = value.intValue();
            lats.set(index, Helper.degreeToInt(lat));
            lons.set(index, Helper.degreeToInt(lon));
        }

        @Override
        public void add(Collection<CoordTrig<Integer>> coll, int edgeId) {
            double lat = Helper.intToDegree(lats.get(edgeId));
            double lon = Helper.intToDegree(lons.get(edgeId));
            coll.add(new CoordTrig<Integer>(lat, lon));
        }
    }

    public Location2NodesNtree(Graph g, Directory dir) {
        this.g = g;
        // 4 * 4 tiles => 2*2^2
        // 8 * 8 tiles => 2*2^3
        // 16 * 16 tiles => 2*2^4        
        n(4);
        maxDepth = 1;
        // (4 * 4)^6 = 16mio        
        dataAccess = dir.findCreate("spatialIndex");
        keyAlgo = new SpatialKeyAlgo(maxDepth * (2 * n));
        // bresenham = new BresenhamLine(, );
    }

    /**
     * @param n with l=2^n where l is the length of an internal tile. If n=2
     * then this is a quadtree.
     */
    Location2NodesNtree n(int n) {
        // this.length = 1 << n;
        this.n = n;
        return this;
    }

    public boolean loadExisting() {
        // TODO NOW
        if (dataAccess.loadExisting()) {
            return true;
        }
        return false;
    }

    public Location2NodesNtree prepareIndex() {
        // TODO NOW bresenheim resolution depends on bounds and spatialkey bits
        RawEdgeIterator iter = g.getAllEdges();
        while (iter.next()) {
            int edge = iter.edge();

            int nodeA = iter.nodeA();
            double lat1 = g.getLatitude(nodeA);
            double lon1 = g.getLongitude(nodeA);
            double lat2;
            double lon2;
            PointList points = iter.wayGeometry();
            int len = points.size();
            for (int i = 0; i < len; i++) {
                lat2 = points.latitude(i);
                lon2 = points.longitude(i);
                addEdge(edge, lat1, lon1, lat2, lon2);
                lat1 = lat2;
                lon1 = lon2;
            }
            int nodeB = iter.nodeB();
            lat2 = g.getLatitude(nodeB);
            lon2 = g.getLongitude(nodeB);
            addEdge(edge, lat1, lon1, lat2, lon2);
        }
        return this;
    }

    public void addEdge(final int edgeId, double lat1, double lon1, double lat2, double lon2) {
        // TODO
//        BresenhamLine.calcPoints(lat1, lon1, lat2, lon2, new PointEmitter() {
//            @Override public void set(double lat, double lon) {
//                addEdge(lat, lon, edgeId);
//            }
//        }, 1, 1); 
    }

    public void addEdge(double lat, double lon, int edge) {
        size++;
        long key = keyAlgo.encode(lat, lon);
        long window = ((long) n * 2) - 1;
        addEdge(0, key, window, 0, edge);
    }

    /**
     * TODO how to create a new area? or adding to old area?
     */
    private void addEdge(int pointer, long key, long window, int depth, int edge) {
        if (depth < maxDepth) {
            // create new tree segments
            long index = (key & window) >>> (depth * n);
            dataAccess.ensureCapacity(index * 4);
            int ref = dataAccess.getInt(pointer + index);
            if (ref < 0) {
                ref -= ref;
                addEdge(ref, key, window << n, depth++, edge);
            } else {
                dataAccess.setInt(index, edge);
            }
        } else {
            // create new list segment
            // TODO list size can increase at a later time
        }
    }

    @Override
    public TIntList findIDs(GHPlace point, EdgeFilter edgeFilter) {
        TIntArrayList list = new TIntArrayList();
        return list;
    }

    @Override
    public int findID(double lat, double lon) {
        TIntList list = findIDs(new GHPlace(lat, lon), ALL_EDGES);
        if (list.isEmpty())
            return -1;
        return list.get(0);
    }

    @Override
    public Location2IDIndex prepareIndex(int capacity) {
        prepareIndex();
        return this;
    }

    @Override
    public Location2IDIndex precision(boolean approxDist) {
        return this;
    }

    @Override
    public float calcMemInMB() {
        return 0;
    }

    @Override
    public QuadTree init(long maxItemsHint) {
        return this;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void add(double lat, double lon, Number value) {
        addEdge(lat, lon, value.intValue());
    }

    @Override
    public int remove(double lat, double lon) {
        // size--;
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<CoordTrig<Number>> getNodesFromValue(double lat, double lon, Number value) {
        // resolver.add(result, foundEdgeId);
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<CoordTrig<Number>> getNodes(double lat, double lon, double distanceInKm) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<CoordTrig<Number>> getNodes(Shape boundingBox) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        size = 0;
    }

    @Override
    public String toDetailString() {
        return "";
    }

    @Override
    public long getMemoryUsageInBytes(int factor) {
        return dataAccess.capacity();
    }

    @Override
    public long getEmptyEntries(boolean onlyBranches) {
        return -1;
    }
}
