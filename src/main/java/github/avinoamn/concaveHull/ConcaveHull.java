package github.avinoamn.concaveHull;

import java.util.*;
import java.util.stream.IntStream;
import org.locationtech.jts.geom.*;
import github.avinoamn.concaveHull.utils.Utils;

/**
 * Computes the concave hull of a {@link Geometry}.
 * The concave hull is the smallest concave Geometry that contains all the
 * points in the input Geometry without self intersection.
 */
public class ConcaveHull {
    private static GeometryFactory factory;

    private static Coordinate[] LSCoords;
    private static List<Coordinate> validCoords = new ArrayList<Coordinate>();

    private static boolean isLRing;                 // Is `lineString` a linearRing (closing itself)
    
    private static boolean isCurrLineFirst;         // Is `currLine` the first line of `lineString`
    private static boolean isIterationLineLast;     // Is `iterationLine` the last line of `lineString`

    private static boolean isCurrLineStart;         // Is the intersection at the start of `currLine`
    private static boolean isCurrLineEnd;           // Is the intersection at the end of `currLine`
    private static boolean isIterationLineStart;    // Is the intersection at the start of `iterationLine`
    private static boolean isIterationLineEnd;      // Is the intersection at the end of `iterationLine`

    /**
     * 
     */
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
    
    /**
     * 
     */
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

    /**
     * 
     */
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

    /**
     * 
     */
    public static LinearRing concaveHull(LinearRing linearRing) {
        GeometryFactory factory = linearRing.getFactory();

        LineString lineString = factory.createLineString(linearRing.getCoordinateSequence());
        LineString fixedLineString = concaveHull(lineString);

        return factory.createLinearRing(fixedLineString.getCoordinateSequence());
    }

    /**
     * 
     */
    public static LineString concaveHull(LineString lineString) {
        factory = lineString.getFactory();
        isLRing = lineString.getStartPoint().equals(lineString.getEndPoint());
        
        return getConcaveHull(lineString, 0);
    }

    /**
     * Recursive function that iterates over the lines of `lineString`
     * and computes hull for current line intersection with the iteration lines
     */
    private static LineString getConcaveHull(LineString lineString, int currIndex) {
        if (lineString.getNumPoints() - 3 <= currIndex) {
            return lineString;
        } else {
            LSCoords = lineString.getCoordinates();

            validCoords.clear();
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

            return getConcaveHull(fixedLS, currIndex + 1);
        }
    }

    /**
     * 
     */
    private static void computeHull(LineString currLine, int currIndex, LineString iterationLine, int iterationIndex, Coordinate intersection) {
        linesIntersectionInfo(currLine, currIndex, iterationLine, iterationIndex, intersection);

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

    /**
     * Set lines intersection information to the fitting class variables 
     */
    private static void linesIntersectionInfo(LineString currLine, int currIndex, LineString iterationLine, int iterationIndex, Coordinate intersection) {
        isCurrLineFirst = currIndex == 0;
        isIterationLineLast = iterationIndex == LSCoords.length - 2;

        isCurrLineStart = currLine.getStartPoint().getCoordinate().equals(intersection);
        isCurrLineEnd = currLine.getEndPoint().getCoordinate().equals(intersection);
        isIterationLineStart = iterationLine.getStartPoint().getCoordinate().equals(intersection);
        isIterationLineEnd = iterationLine.getEndPoint().getCoordinate().equals(intersection);
    }

    /**
     * Return a {@link boolean} that represents whether we need to compute hull or not.
     * 
     * @return if there`s no intersection, a {@value false}
     * if we already computed the hull in a previous line, a {@value false}
     * if there's no following/previous line to compute the hull with, a {@value false} (can happen when isLRing equals false)
     * else, a {@value true}
     */
    private static boolean intersects(Coordinate intersection) {
        return !(
            intersection == null ||
            isIterationLineStart ||
            (isIterationLineEnd && isIterationLineLast) ||
            (isCurrLineStart && ((isCurrLineFirst && !isLRing) || !isCurrLineFirst))
        );
    }
}