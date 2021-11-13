package github.avinoamn.concaveHull.utils;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.algorithm.*;

public class Utils {
    /**
     * @return the clockwise angle between two tips and their tail (in degrees), a {@link double}
     */
    public static double angleBetweenClockwise(Coordinate tip1, Coordinate tail, Coordinate tip2) {
        return (360 - Angle.toDegrees(Angle.angleBetweenOriented(tip1, tail, tip2))) % 360;
    }

    /**
     * Check if the two lines are crossing or only touching.
     * line 1: (head1) -> (intersection) -> (tail1)
     * line 2: (head2) -> (intersection) -> (tail2)
     * 
     * @return if the lines are crossing, a {@value true}
     * else, a {@value false}
     */
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
