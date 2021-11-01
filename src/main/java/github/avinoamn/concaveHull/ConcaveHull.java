package github.avinoamn.concaveHull;

import java.util.*;
import java.util.stream.IntStream;
import org.locationtech.jts.geom.*;

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

    private static LinearRing concaveHull(LinearRing linearRing, int currCoordinateIndex) {
        if (linearRing.getNumPoints() - 2 == currCoordinateIndex) {
            return linearRing;
        } else {
            GeometryFactory factory = linearRing.getFactory();

            Coordinate[] linearRingCoordinates = linearRing.getCoordinates();

            LineString currLine = factory.createLineString(new Coordinate[]
                {linearRingCoordinates[currCoordinateIndex], linearRingCoordinates[currCoordinateIndex + 1]});

            List<Coordinate> validCoordinates = Arrays.asList(Arrays.copyOfRange(linearRingCoordinates, currCoordinateIndex + 1, currCoordinateIndex + 3));
            List<Coordinate> linearRingHead = Arrays.asList(Arrays.copyOfRange(linearRingCoordinates, 0, currCoordinateIndex + 1));
            List<Coordinate> linearRingTail = Arrays.asList(Arrays.copyOfRange(linearRingCoordinates, currCoordinateIndex + 2, linearRingCoordinates.length));

            for (int testCoordinateIndex = 0; testCoordinateIndex < linearRingTail.size() - 1; testCoordinateIndex++) {
                LineString intersectionTestLine = factory.createLineString(new Coordinate[]
                    {linearRingCoordinates[testCoordinateIndex], linearRingCoordinates[testCoordinateIndex + 1]});

                Point intersectionPoint = (Point) currLine.intersection(intersectionTestLine);
                Coordinate intersectionCoordinate = intersectionPoint.getCoordinate();

                if (!intersectionPoint.isEmpty()) {
                    Collections.reverse(validCoordinates);
                    validCoordinates.add(0, intersectionCoordinate);
                    validCoordinates.add(intersectionCoordinate);
                    validCoordinates.add(linearRingTail.get(testCoordinateIndex + 1));
                } else {
                    validCoordinates.add(intersectionCoordinate);
                }
            }

            validCoordinates.addAll(0, linearRingHead);
            LinearRing fixedLinearRing = factory.createLinearRing(validCoordinates.toArray(Coordinate[]::new));

            return concaveHull(fixedLinearRing, currCoordinateIndex + 1);
        }
    }
}