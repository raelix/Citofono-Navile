package citofono.navile.com.citofono;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by raeli on 14/11/2015.
 */

public class Delayer extends Thread {

    private static final int defaultTimeBeforeFireEvent = 15000; //ms
    private static final int maxFalseAlarm = 25; //ms

    private int timeBeforeFireEvent ; //ms
    private static final int defaultCounterBeforeFireEvent = 250; //times called restart timer before repaint
    private static final Object lockDelay = new Object();

    private Thread currentThread;
    private AtomicInteger counter;
    private AtomicInteger falseAlarm;
    private OnCloseMotionListener listener;

    public Delayer(OnCloseMotionListener listener, int timeBeforeFire) {
        this.counter = new AtomicInteger(0);
        this.falseAlarm = new AtomicInteger(0);
        this.listener = listener;
        if(timeBeforeFire == -1)
            this.timeBeforeFireEvent = defaultTimeBeforeFireEvent;
        else
            this.timeBeforeFireEvent = timeBeforeFire;
        System.out.println("timeBeforeFireEvent setted: "+timeBeforeFireEvent);
    }

    public void setDefaultTimeBeforeFireEvent(int time){
        this.timeBeforeFireEvent = time;
    }


    @Override
    public void run() {
        super.run();
        synchronized (lockDelay) {
            try{
                System.out.println("DelayedManager: waiting "+timeBeforeFireEvent+" ms!");
                Thread.sleep(timeBeforeFireEvent);
            }catch(InterruptedException e){
                return;
            }
            fireEvent();
        }
    }

    public void restartTimer(){
        if(currentThread != null)
            currentThread.interrupt();
        //evita starvation se la funzione restartTimer viene chiamata pi di defaultCounterBeforeFireEvent
        //l'evento viene forzato senza attesa e il counter resettato
        if(counter.incrementAndGet() >= defaultCounterBeforeFireEvent){
            fireEvent();
        }
        else{
            currentThread = new Thread(this);
            currentThread.start();
        }
    }


    private void fireEvent(){
        listener.closeCommunication();
        if(counter.get() == 1){
            System.out.println("falso allarme");
            falseAlarm.incrementAndGet();
        }
        resetCounter();
        if(falseAlarm.get() >= maxFalseAlarm){
            falseAlarm.set(0);
            listener.incrementThreshold();
        }
        System.out.println("DelayedManager: fire event!");
    }

    private void resetCounter(){
        counter.set(0);
    }


}

