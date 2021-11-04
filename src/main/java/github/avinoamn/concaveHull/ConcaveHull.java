package github.avinoamn.concaveHull;

import java.util.*;
import java.util.stream.IntStream;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.algorithm.*;

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
        return concaveHull(linearRing, 0);
    }

    private static LinearRing concaveHull(LinearRing linearRing, int currCoordIndex) {
        if (linearRing.getNumPoints() - 3 <= currCoordIndex) {
            return linearRing;
        } else {
            GeometryFactory factory = linearRing.getFactory();

            Coordinate[] LRingCoords = linearRing.getCoordinates();

            LineString currLine = factory.createLineString(new Coordinate[] {LRingCoords[currCoordIndex], LRingCoords[currCoordIndex + 1]});

            List<Coordinate> LRingHead = Arrays.asList(Arrays.copyOfRange(LRingCoords, 0, currCoordIndex + 1));
            List<Coordinate> LRingTail = Arrays.asList(Arrays.copyOfRange(LRingCoords, currCoordIndex + 2, LRingCoords.length));

            List<Coordinate> validCoords = new ArrayList<Coordinate>();
            validCoords.add(LRingCoords[currCoordIndex + 1]);
            validCoords.add(LRingCoords[currCoordIndex + 2]);

            int forLoopIterations = LRingTail.size() - (currCoordIndex == 0 ? 1 : 0) - 1;
            for (int testCoordIndex = 0; testCoordIndex < forLoopIterations; testCoordIndex++) {
                LineString intersectionTestLine = factory.createLineString(new Coordinate[]
                    {LRingTail.get(testCoordIndex), LRingTail.get(testCoordIndex + 1)});

                Coordinate intersectionCoord = currLine.intersection(intersectionTestLine).getCoordinate();
                
                if (intersectionCoord != null) {
                    boolean isCurrLineStart = currLine.getStartPoint().getCoordinate().equals(intersectionCoord);
                    boolean isCurrLineEnd = currLine.getEndPoint().getCoordinate().equals(intersectionCoord);
                    boolean isIntersectionTestLineStart = intersectionTestLine.getStartPoint().getCoordinate().equals(intersectionCoord);
                    boolean isIntersectionTestLineEnd = intersectionTestLine.getEndPoint().getCoordinate().equals(intersectionCoord);

                    Coordinate head1 = intersectionTestLine.getStartPoint().getCoordinate();
                    
                    Coordinate tail1 = isIntersectionTestLineEnd ?
                        LRingTail.get(testCoordIndex + 2) :
                        intersectionTestLine.getEndPoint().getCoordinate();
                    
                    Coordinate head2 = isCurrLineStart ?
                        currLine.getEndPoint().getCoordinate() :
                        currLine.getStartPoint().getCoordinate();
                    
                    Coordinate tail2 = !isCurrLineStart && !isCurrLineEnd ?
                        currLine.getEndPoint().getCoordinate() :
                        (isCurrLineEnd ?
                            LRingCoords[currCoordIndex + 2] :
                            LRingCoords[currCoordIndex == 0 ? LRingCoords.length - 2 : currCoordIndex - 1]);

                    if (!isIntersectionTestLineStart && crosses(head1, tail1, head2, tail2, intersectionCoord)) {
                        Collections.reverse(validCoords);
                        validCoords.add(intersectionCoord);
                        if (!isCurrLineStart) validCoords.add(0, intersectionCoord);
                        if (!isIntersectionTestLineEnd) validCoords.add(tail1);
                    } else {
                        validCoords.add(isIntersectionTestLineEnd ? intersectionCoord : tail1);
                    }
                } else {
                    validCoords.add(intersectionTestLine.getEndPoint().getCoordinate());
                }
            }

            validCoords.addAll(0, LRingHead);
            if (currCoordIndex == 0) validCoords.add(LRingCoords[0]);
            LinearRing fixedLRing = factory.createLinearRing(validCoords.toArray(Coordinate[]::new));

            return concaveHull(fixedLRing, currCoordIndex + 1);
        }
    }

    public static double angleBetweenClockwise(Coordinate tip1, Coordinate tail, Coordinate tip2) {
        return (360 - Angle.toDegrees(Angle.angleBetweenOriented(tip1, tail, tip2))) % 360;
    }

    public static boolean crosses(Coordinate head1, Coordinate tail1, Coordinate head2, Coordinate tail2, Coordinate intersection) {
        double a1 = angleBetweenClockwise(head1, intersection, head2);
        double a2 = angleBetweenClockwise(head1, intersection, tail2);
        double a3 = angleBetweenClockwise(head1, intersection, tail1);

        if (a1 < a2) {
            return a3 >= a1 && a3 <= a2;
        } else {
            return a3 >= a2 && a3 <= a1;
        }
    }
}