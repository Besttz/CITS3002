/** // Tommy Chenhao Zhang, 22181467
 * Routes saved for time table
 */
public class Route {
    String from;
    int departH;
    int departM;
    String name;
    String platform;
    int arriveH;
    int arriveM;
    String destination;

    /**
     * Create a new route info.
     */
    public Route(String fromARG, int departHARG, int departMARG,String nARG, String platARG, int arriveHARG, int arriveMARG,
            String destinationARG) {
        from = fromARG;
        departH = departHARG;
        departM = departMARG;
        name = nARG;
        platform = platARG;
        arriveH = arriveHARG;
        arriveM = arriveMARG;
        destination = destinationARG;
    }

}