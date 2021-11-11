package github.avinoamn.concaveHull.utils;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.algorithm.*;

public class Utils {
    /**
     * 
     * @param tip1
     * @param tail
     * @param tip2
     * @return
     */
    public static double angleBetweenClockwise(Coordinate tip1, Coordinate tail, Coordinate tip2) {
        return (360 - Angle.toDegrees(Angle.angleBetweenOriented(tip1, tail, tip2))) % 360;
    }

    /**
     * 
     * @param head1
     * @param tail1
     * @param head2
     * @param tail2
     * @param intersection
     * @return
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
