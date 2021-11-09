package github.avinoamn.concaveHull;

import java.util.*;
import java.util.stream.IntStream;
import org.locationtech.jts.geom.*;
import github.avinoamn.concaveHull.utils.Utils;

public class ConcaveHull {
    public static MultiPolygon concaveHull(MultiPolygon multiPolygon) {
        GeometryFactory factory = multiPolygon.getFactory();
        List<Polygon> fixedPolygons = new ArrayList<Polygon>();

        IntStream.range(0, multiPolygon.getNumGeometries()).forEach(i -> {
            Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
            Polygon fixedPolygon = concaveHull(polygon);
            fixedPolygons.add(fixedPolygon);
        });
        
        return factory.createMultiPolygon(fixedPolygons.toArray(Polygon[]::new));
    }
    
    public static Polygon concaveHull(Polygon polygon) {
        GeometryFactory factory = polygon.getFactory();
        Geometry boundary = polygon.getBoundary();
        List<LinearRing> fixedLinearRings = new ArrayList<LinearRing>();

        IntStream.range(0, boundary.getNumGeometries()).forEach(i -> {
            LinearRing linearRing = (LinearRing) boundary.getGeometryN(i);
            LinearRing fixedLinearRing = concaveHull(linearRing);
            fixedLinearRings.add(fixedLinearRing);
        });
        
        if (fixedLinearRings.size() == 1) {
            return factory.createPolygon(fixedLinearRings.get(0));
        } else if (fixedLinearRings.size() > 1) {
            return factory.createPolygon(
                fixedLinearRings.get(0),
                fixedLinearRings.subList(1, fixedLinearRings.size()).toArray(LinearRing[]::new));
        } else {
            return polygon;
        }
    }

    public static LinearRing concaveHull(LinearRing linearRing) {
        GeometryFactory factory = linearRing.getFactory();

        LineString lineString = factory.createLineString(linearRing.getCoordinateSequence());
        LineString fixedLineString = concaveHull(lineString);

        return factory.createLinearRing(fixedLineString.getCoordinateSequence());
    }

    public static LineString concaveHull(LineString lineString) {
        boolean isLRing = lineString.getStartPoint().equals(lineString.getEndPoint());
        return concaveHull(lineString, 0, isLRing);
    }

    private static LineString concaveHull(LineString lineString, int currCoordIndex, boolean isLRing) {
        if (lineString.getNumPoints() - 3 <= currCoordIndex) {
            return lineString;
        } else {
            GeometryFactory factory = lineString.getFactory();

            Coordinate[] LSCoords = lineString.getCoordinates();

            LineString currLine = factory.createLineString(new Coordinate[] {LSCoords[currCoordIndex], LSCoords[currCoordIndex + 1]});

            List<Coordinate> LSHead = Arrays.asList(Arrays.copyOfRange(LSCoords, 0, currCoordIndex + 1));
            List<Coordinate> LSTail = Arrays.asList(Arrays.copyOfRange(LSCoords, currCoordIndex + 2, LSCoords.length));

            List<Coordinate> validCoords = new ArrayList<Coordinate>();
            validCoords.add(LSCoords[currCoordIndex + 1]);
            validCoords.add(LSCoords[currCoordIndex + 2]);

            for (int testCoordIndex = 0; testCoordIndex < LSTail.size() - 1; testCoordIndex++) {
                LineString intersectionTestLine = factory.createLineString(new Coordinate[]
                    {LSTail.get(testCoordIndex), LSTail.get(testCoordIndex + 1)});

                Coordinate intersectionCoord = currLine.intersection(intersectionTestLine).getCoordinate();
                
                boolean isCurrLineFirst = currCoordIndex == 0;
                boolean isCurrLineStart = currLine.getStartPoint().getCoordinate().equals(intersectionCoord);
                boolean isCurrLineEnd = currLine.getEndPoint().getCoordinate().equals(intersectionCoord);
                boolean isIntersectionTestLineLast = testCoordIndex == LSTail.size() - 2;
                boolean isIntersectionTestLineStart = intersectionTestLine.getStartPoint().getCoordinate().equals(intersectionCoord);
                boolean isIntersectionTestLineEnd = intersectionTestLine.getEndPoint().getCoordinate().equals(intersectionCoord);

                boolean intersects = !(
                    intersectionCoord == null ||
                    isIntersectionTestLineStart ||
                    (isIntersectionTestLineEnd && isIntersectionTestLineLast) ||
                    (isCurrLineStart && ((isCurrLineFirst && !isLRing) || !isCurrLineFirst))
                );

                if (intersects) {
                    Coordinate intersectionTestLineHead = intersectionTestLine.getStartPoint().getCoordinate();
                    
                    Coordinate intersectionTestLineTail = isIntersectionTestLineEnd ?
                        LSTail.get(testCoordIndex + 2) :
                        intersectionTestLine.getEndPoint().getCoordinate();
                    
                    Coordinate currLineHead = isCurrLineStart ?
                        LSCoords[isCurrLineFirst ? LSCoords.length - 2 : currCoordIndex - 1] :
                        currLine.getStartPoint().getCoordinate();
                    
                    Coordinate currLineTail = isCurrLineEnd ?
                        LSCoords[currCoordIndex + 2] :
                        currLine.getEndPoint().getCoordinate();

                    if (Utils.crosses(intersectionTestLineHead, intersectionTestLineTail, currLineHead, currLineTail, intersectionCoord)) {
                        Collections.reverse(validCoords);
                        validCoords.add(intersectionCoord);
                        if (!isCurrLineStart) validCoords.add(0, intersectionCoord);
                        if (!isIntersectionTestLineEnd) validCoords.add(currLineTail);
                    } else {
                        validCoords.add(isIntersectionTestLineEnd ? intersectionCoord : currLineTail);
                    }
                } else {
                    validCoords.add(intersectionTestLine.getEndPoint().getCoordinate());
                }
            }

            validCoords.addAll(0, LSHead);
            LineString fixedLS = factory.createLineString(validCoords.toArray(Coordinate[]::new));

            return concaveHull(fixedLS, currCoordIndex + 1, isLRing);
        }
    }
}