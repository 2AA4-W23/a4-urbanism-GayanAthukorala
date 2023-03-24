package island.files;

import ca.mcmaster.cas.se2aa4.a2.io.Structs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Rivers {
    List<Integer> islandBlocks = new ArrayList<>();
    List<Integer> islandVertices = new ArrayList<>();
    List<Double> vertexHeights;
    List<Double> elevations;
    List<Structs.Polygon> polygonList;
    List<Structs.Segment> segmentList;
    List<Structs.Vertex> vertexList;
    int riverNum;
    int riverStartIdx;
    public void generate(int rM, int rSI, List<Structs.Polygon> pList ,List<Structs.Segment> sList,List<Structs.Vertex> vList, List<Double> e,List<Double> vH, List<Integer> iV, List<Integer> iB){
        islandBlocks = iB;
        elevations = e;
        vertexHeights = vH;
        islandVertices = iV;
        vertexList = vList;
        segmentList = sList;
        polygonList = pList;
        riverNum = rM;
        riverStartIdx = rSI;

        generateVTPRelation();
        getVTVRelation();
        generateVertexHeights();
        riverFlow();
    }
    private void generateVertexHeights(){
        for (Integer vertIdx : islandVertices){
            List<Integer> polysAssociated = VTPRelations.get(vertIdx);
            Double lowestValue = null;
            for (Integer polyIdx : polysAssociated){
                Double height = elevations.get(polyIdx);
                if (lowestValue == null){
                    lowestValue = height;
                    continue;
                }
                if (height < lowestValue){
                    lowestValue = height;
                }
            }
            vertexHeights.set(vertIdx,lowestValue);
        }
    }

    List<List<Integer>> VTVRelations;
    List<List<Integer>> VTSRelations;
    private void getVTVRelation(){
        VTVRelations = new ArrayList<>(Collections.nCopies(vertexList.size(),new ArrayList<>()));
        VTSRelations = new ArrayList<>(Collections.nCopies(vertexList.size(),new ArrayList<>()));
        for (Structs.Segment s : segmentList){
            int v1Idx = s.getV1Idx();
            int v2Idx = s.getV2Idx();
            int segID = segmentList.indexOf(s);
            if (!VTVRelations.get(v1Idx).contains(v2Idx) && !VTSRelations.get(v1Idx).contains(segID)){
                List<Integer> sList = new ArrayList<>(VTVRelations.get(v1Idx));
                sList.add(v2Idx);
                VTVRelations.set(v1Idx,sList);

                List<Integer> vList = new ArrayList<>(VTSRelations.get(v1Idx));
                vList.add(segID);
                VTSRelations.set(v1Idx,vList);
            }
            if (!VTVRelations.get(v2Idx).contains(v1Idx) && !VTSRelations.get(v2Idx).contains(segID)){
                List<Integer> sList = new ArrayList<>(VTVRelations.get(v2Idx));
                sList.add(v1Idx);
                VTVRelations.set(v2Idx,sList);

                List<Integer> vList = new ArrayList<>(VTSRelations.get(v2Idx));
                vList.add(segID);
                VTSRelations.set(v2Idx,vList);
            }
        }
    }

    private void riverFlow(){
        Random rand = new Random();
        for (int i = 0; i < riverNum; i++){
            boolean notEnd = true;
            int randIslandVert = riverStartIdx+i;
            int id = islandVertices.get(randIslandVert);
            while (notEnd) {
                List<Integer> vertNeighbours = VTVRelations.get(id);
                Integer nextSeg = null;
                Integer nextVert = null;
                double height = vertexHeights.get(id);
                for (int j = 0; j < vertNeighbours.size(); j++) {
                    double compare = vertexHeights.get(vertNeighbours.get(j));
                    if (vertexHeights.get(vertNeighbours.get(j)) < height) {
                        height = compare;
                        nextSeg = VTSRelations.get(id).get(j);
                        nextVert = vertNeighbours.get(j);
                    }
                }
                if (nextSeg == null) {
                    notEnd = false;
                    colorVertex(vertexList.get(id),0,0,255,255);
                    break;
                }
                colorSegment(segmentList.get(nextSeg),0,0,255,255);
                id = nextVert;
            }
        }
    }

    private void colorSegment(Structs.Segment seg, int red, int green, int blue, int alpha){
        // Create new Property with "rgb_color" key and the rgb value as the value
        Structs.Property color = Structs.Property.newBuilder().setKey("rgb_color").setValue(red + "," + green + "," + blue+ "," + alpha).build();
        Structs.Segment colored = Structs.Segment.newBuilder(seg).addProperties(color).build();
        // Set the old segment in the list as the new one with color property
        segmentList.set(segmentList.indexOf(seg), colored);
    }
    List<List<Integer>> VTPRelations;
    private void generateVTPRelation(){
        VTPRelations = new ArrayList<>(Collections.nCopies(vertexList.size(),new ArrayList<>()));
        for (Integer polyIdx : islandBlocks){
            Structs.Polygon poly = polygonList.get(polyIdx);
            List<Integer> vertice = extractVertices(poly.getPropertiesList());
            for (Integer v : vertice){
                List<Integer> vList = VTPRelations.get(v);
                if (!vList.contains(polyIdx)){
                    List<Integer> vList2 = new ArrayList<>(vList);
                    vList2.add(polyIdx);
                    VTPRelations.set(v,vList2);
                }
            }
        }
    }
    private List<Integer> extractVertices(List<Structs.Property> properties){
        String val = null;
        for(Structs.Property p: properties) {
            // TRY TO FIND THE RGB COLOR
            if (p.getKey().equals("vertices")) {
                val = p.getValue();
            }
        }
        if (val == null){       // IF THE RGB COLOR PROPERTY DOESNT EXIST, COVER THAT CASE BY MAKING IT BLACK
            System.out.println("NO VERTEX PROPERTY");
            return null;
        }
        String[] raw = val.split(",");
        List<Integer> rawInts = new ArrayList<>();
        for (int i =0; i< raw.length;i++){
            Integer value = Integer.parseInt(raw[i]);
            rawInts.add(value);
        }
        return rawInts;
    }
    /**
     * This function replaces the vertex given as a parameter and adds a rgb color property to it based on rgb value params
     * @param vertex
     * @param red
     * @param green
     * @param blue
     * @param alpha
     */
    private void colorVertex(Structs.Vertex vertex, int red, int green, int blue, int alpha){
        String colorCode = red + "," + green + "," + blue + "," + alpha;
        // Create new Property with "rgb_color" key and the rgb value as the value
        Structs.Property color = Structs.Property.newBuilder().setKey("rgb_color").setValue(colorCode).build();
        Structs.Vertex colored = Structs.Vertex.newBuilder(vertex).addProperties(color).build();
        // Set the old vertex in the list as the new one with color property
        vertexList.set(vertexList.indexOf(vertex), colored);
    }
}