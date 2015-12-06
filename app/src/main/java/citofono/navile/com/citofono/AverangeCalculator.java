package citofono.navile.com.citofono;

import org.apache.commons.collections4.queue.CircularFifoQueue;

/**
 * Created by raeli on 08/11/2015.
 */
public class AverangeCalculator {
    private static final int defaultThreshold = 13;

    //private static final int defaultThreshold = 5;
    private  int threshold ;
    private static final int starterSize = 6;
    private static final int finisherSize = 4;
    CircularFifoQueue<Double> starterList;
    CircularFifoQueue<Double> finisherList;
    double lastDiff = 0;
    double lastlastDiff = 0;
    OnBellRingListener listener;
    boolean stopped = false;

    public AverangeCalculator(OnBellRingListener listener, int tsh){
        starterList = new CircularFifoQueue<>(starterSize);
        finisherList = new CircularFifoQueue<>(finisherSize);
        this.listener = listener;
        if(tsh == -1) {
            this.threshold = defaultThreshold;
        }
        else {
            this.threshold = tsh;
        }
        System.out.println("Threshold setted: "+threshold);
    }

    public void addValue(double value){
        stopped = false;
        if(starterList.size() != starterSize){
            starterList.add(value);
        }
        else{
            if(finisherList.size() != finisherSize){
                finisherList.add(value);
            }
            else{
                compare();
                moveValues();
            }
        }
    }

    public int getThreshold(){
        return this.threshold;
    }

    public void changeThreshold(int trh){
        this.threshold = trh;
    }

    public void incrementThreshold(){
        this.threshold++;
    }

    public void stop(){
        stopped = true;
        starterList.clear();
        finisherList.clear();

    }

    private void compare(){
        double starterAvg = calcAverage(starterList.toArray(new Double[starterList.size()]));
        double finisherAvg = calcAverage(finisherList.toArray(new Double[finisherList.size()]));
        if(finisherList.size() == finisherSize && starterList.size() == starterSize) {
            if (finisherAvg > starterAvg) {
                if (lastlastDiff < lastDiff) {
                    if (lastDiff < (finisherAvg - starterAvg) && (finisherAvg - starterAvg >= threshold)) {
                        if (listener != null)
                            if (!stopped){
                                if(!new String(""+starterAvg).contains("-Infinity") && !new String(""+finisherAvg).contains("-Infinity")) {
                                    System.out.println("Ring " + finisherAvg + " " + starterAvg + " " + finisherList.size() + " " + starterList.size()+" threshold: "+threshold);
                                    listener.bellRing();
                                }
                            }
                    }
                }
                lastlastDiff = lastDiff;
                lastDiff = finisherAvg - starterAvg;
            }
        }
    }

    public double getLastDiff(){
        return lastDiff;
    }

  /*  private void moveValues(){
        if(finisherList.size() > 0)
            if(finisherList.get(0) != null){
                starterList.add(finisherList.get(0));
                finisherList.remove(0);
            }
    }
*/
  private void moveValues(){
      if(finisherList.size() > 0)
              starterList.add(finisherList.poll());

  }
    public static double calcAverage(Double[] people) {
        double sum = 0;
        double length= 0;
        for (int i=0; i < people.length; i++) {
            if(people[i] != null) {
                sum = sum + people[i];
            length++;
            }
        }
        // remove the ()
        double result = sum / length;

        // return something
        return result;
    }

}
