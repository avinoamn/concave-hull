package github.avinoamn.concaveHull;

import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

/**
 * Unit test for concave hull.
 */

@TestInstance(Lifecycle.PER_CLASS)
public class ConcaveHullUnitTest
{
    private GeometryFactory factory;
    private LinearRing[] testLinearRings, resultLinearRings;
    private Polygon testPolygon, resultPolygon;
    private MultiPolygon testMultiPolygon, resultMultiPolygon;

    @BeforeAll
    void setup() {
        factory = new GeometryFactory();

        testLinearRings = new LinearRing[]{
            factory.createLinearRing(new Coordinate[]{
                new Coordinate(-1, -1),
                new Coordinate(2, -1),
                new Coordinate(2, 2),
                new Coordinate(-1, 2),
                new Coordinate(-1, -1)
            }),
            factory.createLinearRing(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 0),
                new Coordinate(0, 1),
                new Coordinate(1, 1),
                new Coordinate(0, 0)
            })
        };

        resultLinearRings = new LinearRing[]{
            testLinearRings[0],
            factory.createLinearRing(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 0),
                new Coordinate(0.5, 0.5),
                new Coordinate(1, 1),
                new Coordinate(0, 1),
                new Coordinate(0.5, 0.5),
                new Coordinate(0, 0)
            })
        };

        testPolygon = factory.createPolygon(
            testLinearRings[0], Arrays.copyOfRange(testLinearRings, 1, testLinearRings.length));

        resultPolygon = factory.createPolygon(
            resultLinearRings[0], Arrays.copyOfRange(resultLinearRings, 1, resultLinearRings.length));

        testMultiPolygon = factory.createMultiPolygon(new Polygon[]{testPolygon});
        resultMultiPolygon = factory.createMultiPolygon(new Polygon[]{resultPolygon});
    }

    @Test
    public void ShouldFixLinearRing()
    {
        assertTrue(resultLinearRings[1].equals(ConcaveHull.concaveHull(testLinearRings[1])));
    }

    @Test
    public void ShouldFixPolygon()
    {   
        Polygon fixedPolygon = ConcaveHull.concaveHull(testPolygon);
        assertTrue(resultPolygon.equals(fixedPolygon));
    }

    @Test
    public void ShouldFixMultiPolygon()
    {
        assertTrue(resultMultiPolygon.equals(ConcaveHull.concaveHull(testMultiPolygon)));
    }
}
