package citofono.navile.com.citofono;

/**
 * Created by raeli on 04/11/2015.
 */


public class AudioAnalyzer
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //
    AverangeCalculator calculator;
    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "instrument";
    // The desired sampling rate for this analyser, in samples/sec.
    private int sampleRate = 8000;
    // Audio input block size, in samples.
    private int inputBlockSize = 256;
    // The desired decimation rate for this analyser.  Only 1 in
    // sampleDecimate blocks will actually be processed.
    private int sampleDecimate = 1;
    // The desired histogram averaging window.  1 means no averaging.
    private int historyLen = 4;
    // Our audio input device.
    private final AudioReader audioReader;
    // Buffered audio data, and sequence number of the latest block.
    private short[] audioData;
    private long audioSequence = 0;
    // If we got a read error, the error code.
    private int readError = AudioReader.Listener.ERR_OK;
    // Sequence number of the last block we processed.
    private long audioProcessed = 0;
    // Analysed audio spectrum data; history data for each frequency
    // in the spectrum; index into the history data; and buffer for
    // peak frequencies.
    private float[] spectrumData;
    private float[][] spectrumHist;
    private int spectrumIndex;
    // Current signal power level, in dB relative to max. input power.
    private double currentPower = 0f;
    // Temp. buffer for calculated bias and range.
    private float[] biasRange = null;

    public AudioAnalyzer(OnBellRingListener listener,int threshold) {

        audioReader = new AudioReader();
        calculator = new AverangeCalculator(listener, threshold);

        biasRange = new float[2];
    }

    public void changeThreshold(int threshold){
    calculator.changeThreshold(threshold);
    }

    public void incrementThreshold(){
        calculator.incrementThreshold();
    }

    public int getThreshold(){
        return calculator.getThreshold();
    }


    // ******************************************************************** //
    // Configuration.
    // ******************************************************************** //

    /**
     * Set the sample rate for this instrument.
     *
     * @param   rate        The desired rate, in samples/sec.
     */
    public void setSampleRate(int rate) {
        sampleRate = rate;
    }


    /**
     * Set the input block size for this instrument.
     *
     * @param   size        The desired block size, in samples.  Typical
     *                      values would be 256, 512, or 1024.  Larger block
     *                      sizes will mean more work to analyse the spectrum.
     */
    public void setBlockSize(int size) {
        inputBlockSize = size;


        // Allocate the spectrum data.
        spectrumData = new float[inputBlockSize / 2];
        spectrumHist = new float[inputBlockSize / 2][historyLen];
    }




    /**
     * Set the decimation rate for this instrument.
     *
     * @param   rate        The desired decimation.  Only 1 in rate blocks
     *                      will actually be processed.
     */
    public void setDecimation(int rate) {
        sampleDecimate = rate;
    }


    /**
     * Set the histogram averaging window for this instrument.
     *
     * @param   len         The averaging interval.  1 means no averaging.
     */
    public void setAverageLen(int len) {
        historyLen = len;

        // Set up the history buffer.
        spectrumHist = new float[inputBlockSize / 2][historyLen];
        spectrumIndex = 0;
    }


    // ******************************************************************** //
    // Run Control.
    // ******************************************************************** //

    /**
     * The application is starting.  Perform any initial set-up prior to
     * starting the application.  We may not have a screen size yet,
     * so this is not a good place to allocate resources which depend on
     * that.
     */
    public void appStart() {
    }


    /**
     * We are starting the main run; start measurements.
     */
    public void measureStart() {
        audioProcessed = audioSequence = 0;
        readError = AudioReader.Listener.ERR_OK;

        audioReader.startReader(sampleRate, inputBlockSize * sampleDecimate, new AudioReader.Listener() {
            @Override
            public final void onReadComplete(short[] buffer) {
                receiveAudio(buffer);
            }

            @Override
            public void onReadError(int error) {
                handleError(error);
            }
        });
    }


    /**
     * We are stopping / pausing the run; stop measurements.
     */
    public void measureStop() {
        new Thread(){
            @Override
        public void run(){

                audioReader.stopReader();
                calculator.stop();
            }
        }.start();
    }


    /**
     * The application is closing down.  Clean up any resources.
     */
    public void appStop() {
    }








    // ******************************************************************** //
    // Audio Processing.
    // ******************************************************************** //

    /**
     * Handle audio input.  This is called on the thread of the audio
     * reader.
     *
     * @param   buffer      Audio data that was just read.
     */
    private final void receiveAudio(short[] buffer) {
        // Lock to protect updates to these local variables.  See run().
        synchronized (this) {
            audioData = buffer;
            ++audioSequence;
        }
    }


    /**
     * An error has occurred.  The reader has been terminated.
     *
     * @param   error       ERR_XXX code describing the error.
     */
    private void handleError(int error) {
        synchronized (this) {
            readError = error;
        }
    }

    public int getError(){
        return readError;
    }


    // ******************************************************************** //
    // Main Loop.
    // ******************************************************************** //


    public final void doUpdate() {
        short[] buffer = null;
        synchronized (this) {
            if (audioData != null && audioSequence > audioProcessed) {
                audioProcessed = audioSequence;
                buffer = audioData;
            }
        }

        // If we got data, process it without the lock.
        if (buffer != null)
            processAudio(buffer);

        if (readError != AudioReader.Listener.ERR_OK)
            processError(readError);
    }

public double getLastDifference(){
    return calculator.getLastDiff();
}

    /**
     * Handle audio input.  This is called on the thread of the
     * parent surface.
     *
     * @param   buffer      Audio data that was just read.
     */
    private final void processAudio(short[] buffer) {
        // Process the buffer.  While reading it, it needs to be locked.
        synchronized (buffer) {
            // Calculate the power now, while we have the input
            // buffer; this is pretty cheap.
            final int len = buffer.length;


            // If we have a power gauge, calculate the signal power.

            setCurrentPower(SignalPower.calculatePowerDb(buffer, 0, len));
            calculator.addValue(getCurrentPower());
            // If we have a spectrum analyser, set up the FFT input data.
         //   Log.i(this.getClass().getSimpleName(), "dB = " + getCurrentPower() + "\t\t" + Math.round(getCurrentPower()));

            // Tell the reader we're done with the buffer.
            buffer.notify();
        }




    }


    /**
     * Handle an audio input error.
     *
     * @param   error       ERR_XXX code describing the error.
     */
    private final void processError(int error) {

    }





    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    public void setCurrentPower(double currentPower) {
        this.currentPower = currentPower;
    }


    public double getCurrentPower() {
        return currentPower;
    }



}
