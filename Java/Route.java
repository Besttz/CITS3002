/**
 * Routes
 */
public class Route {
    String from;
    int departH;
    int departM;
    Boolean isTrain;
    int arriveH;
    int arriveM;
    String destination;

    public Route(String fromARG, int departHARG, int departMARG, Boolean isTrainARG, int arriveHARG, int arriveMARG,
            String destinationARG) {
        from = fromARG;
        departH = departHARG;
        departM = departMARG;
        isTrain = isTrainARG;
        arriveH = arriveHARG;
        arriveM = arriveMARG;
        destination = destinationARG;
    }

}