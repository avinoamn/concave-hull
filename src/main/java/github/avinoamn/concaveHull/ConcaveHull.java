package github.avinoamn.concaveHull;

import java.util.*;
import java.util.stream.IntStream;
import org.locationtech.jts.geom.*;
import github.avinoamn.concaveHull.utils.Utils;

public class ConcaveHull {
    private static GeometryFactory factory;

    private static Coordinate[] LSCoords;
    private static List<Coordinate> validCoords;

    private static boolean isLRing;
    
    private static boolean isCurrLineFirst;
    private static boolean isCurrLineStart;
    private static boolean isCurrLineEnd;
    private static boolean isIterationLineLast;
    private static boolean isIterationLineStart;
    private static boolean isIterationLineEnd;

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

    public static MultiLineString concaveHull(MultiLineString multiLineString) {
        GeometryFactory factory = multiLineString.getFactory();
        List<LineString> fixedLineStrings = new ArrayList<LineString>();

        IntStream.range(0, multiLineString.getNumGeometries()).forEach(i -> {
            LineString lineString = (LineString) multiLineString.getGeometryN(i);
            LineString fixedLineString = concaveHull(lineString);
            fixedLineStrings.add(fixedLineString);
        });
        
        return factory.createMultiLineString(fixedLineStrings.toArray(LineString[]::new));
    }

    public static LineString concaveHull(LineString lineString) {
        factory = lineString.getFactory();
        isLRing = lineString.getStartPoint().equals(lineString.getEndPoint());
        
        return concaveHull(lineString, 0);
    }

    private static LineString concaveHull(LineString lineString, int currIndex) {
        if (lineString.getNumPoints() - 3 <= currIndex) {
            return lineString;
        } else {
            LSCoords = lineString.getCoordinates();

            validCoords = new ArrayList<Coordinate>();
            validCoords.add(LSCoords[currIndex + 1]);
            validCoords.add(LSCoords[currIndex + 2]);

            LineString currLine = factory.createLineString(new Coordinate[] {LSCoords[currIndex], LSCoords[currIndex + 1]});
            
            for (int iterationIndex = currIndex + 2; iterationIndex < LSCoords.length - 1; iterationIndex++) {
                LineString iterationLine = factory.createLineString(new Coordinate[] {LSCoords[iterationIndex], LSCoords[iterationIndex + 1]});
                Coordinate intersection = currLine.intersection(iterationLine).getCoordinate();

                computeHull(currLine, currIndex, iterationLine, iterationIndex, intersection);
            }

            validCoords.addAll(0, Arrays.asList(Arrays.copyOfRange(LSCoords, 0, currIndex + 1)));
            LineString fixedLS = factory.createLineString(validCoords.toArray(Coordinate[]::new));

            return concaveHull(fixedLS, currIndex + 1);
        }
    }

    private static void computeHull(LineString currLine, int currIndex, LineString iterationLine, int iterationIndex, Coordinate intersection) {
        linesEdgesIntersection(currLine, currIndex, iterationLine, iterationIndex, intersection);

        if (intersects(intersection)) {
            Coordinate currLineHead = isCurrLineStart ?
                LSCoords[isCurrLineFirst ? LSCoords.length - 2 : currIndex - 1] :
                currLine.getStartPoint().getCoordinate();
            
            Coordinate currLineTail = isCurrLineEnd ?
                LSCoords[currIndex + 2] :
                currLine.getEndPoint().getCoordinate();

            Coordinate iterationLineHead = iterationLine.getStartPoint().getCoordinate();
            
            Coordinate iterationLineTail = isIterationLineEnd ?
                LSCoords[iterationIndex + 2] :
                iterationLine.getEndPoint().getCoordinate();

            if (Utils.crosses(currLineHead, currLineTail, iterationLineHead, iterationLineTail, intersection)) {
                Collections.reverse(validCoords);
                validCoords.add(intersection);
                if (!isCurrLineStart) validCoords.add(0, intersection);
                if (!isIterationLineEnd) validCoords.add(iterationLineTail);
            } else {
                validCoords.add(isIterationLineEnd ? intersection : iterationLineTail);
            }
        } else {
            validCoords.add(iterationLine.getEndPoint().getCoordinate());
        }
    }

    private static void linesEdgesIntersection(LineString currLine, int currIndex, LineString iterationLine, int iterationIndex, Coordinate intersection) {
        isCurrLineFirst = currIndex == 0;
        isCurrLineStart = currLine.getStartPoint().getCoordinate().equals(intersection);
        isCurrLineEnd = currLine.getEndPoint().getCoordinate().equals(intersection);
        isIterationLineLast = iterationIndex == LSCoords.length - 2;
        isIterationLineStart = iterationLine.getStartPoint().getCoordinate().equals(intersection);
        isIterationLineEnd = iterationLine.getEndPoint().getCoordinate().equals(intersection);
    }

    private static boolean intersects(Coordinate intersection) {
        return !(
            intersection == null ||
            isIterationLineStart ||
            (isIterationLineEnd && isIterationLineLast) ||
            (isCurrLineStart && ((isCurrLineFirst && !isLRing) || !isCurrLineFirst))
        );
    }
}