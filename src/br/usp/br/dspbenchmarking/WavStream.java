package br.usp.br.dspbenchmarking;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.os.SystemClock;
import android.util.Log;
import br.usp.br.dspbenchmarking.DspThread.DspCallback;


public class WavStream extends AudioStream {

    // Wav file info
	private int channels;
	private int sampleRate;
	private InputStream wavStream;
	private int dataSizeInBytes = 0;
	
	// Contants
	//private static final String RIFF_HEADER = "RIFF";
	//private static final String WAVE_HEADER = "WAVE";
	//private static final String FMT_HEADER = "fmt ";
	//private static final String DATA_HEADER = "data";
	private static final int HEADER_SIZE = 44;
	//private static final String CHARSET = "ASCII";
	
	// Buffers
	ShortBuffer dataBuffer = null;
	int blockSize;
	
	// Scheduling
	ScheduledExecutorService scheduler;
	ScheduledFuture<?> dspHandle;
	



	/**
	 * Constructor
	 * @throws FileNotFoundException 
	 */
	public WavStream(InputStream is, int bSize) throws FileNotFoundException, IOException {
		blockSize = bSize;

		// Initialize scheduler
		scheduler = Executors.newScheduledThreadPool(1);

		//wavStream = new BufferedInputStream(new FileInputStream(filePath));
		wavStream = new BufferedInputStream(is);
		wavStream.mark(HEADER_SIZE);

		// Initialize for reading
		readHeader();
		wavStream.reset();
		initWavPcm();
	}


	/**
	 * Reads the Wav Header.
	 * @throws IOException 
	 */
	private void readHeader() throws IOException {

		ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		wavStream.read(buffer.array(), buffer.arrayOffset(), buffer.capacity());

		buffer.rewind();
		
		buffer.position(buffer.position() + 20);
		
		//int subChunkSize = buffer.getInt();
		//Log.e("WavStream", "subChunkSize="+subChunkSize);
		
		int format = buffer.getShort();
		checkFormat(format == 1, "Unsupported encoding: " + format); // 1 means
																		// Linear
																		// PCM
		channels = buffer.getShort();
		checkFormat(channels == 1, "Unsupported channels: "
				+ channels);
		sampleRate = buffer.getInt();
		checkFormat(sampleRate <= 48000 && sampleRate >= 11025, "Unsupported rate: " + sampleRate);
		buffer.position(buffer.position() + 6);
		int bits = buffer.getShort();
		checkFormat(bits == 16, "Unsupported bits: " + bits);
		while (buffer.getInt() != 0x61746164) { // "data" marker
			//Log.d("WavInfo", "Skipping non-data chunk");
			//int size = buffer.getInt();
			//wavStream.skip(size);
			//buffer.rewind();
			//wavStream.read(buffer.array(), buffer.arrayOffset(), 8);
			//buffer.rewind();
		}
		dataSizeInBytes = buffer.getInt();
		checkFormat(dataSizeInBytes > 0, "wrong datasize: " + dataSizeInBytes);

		wavStream.reset();
		wavStream.mark(HEADER_SIZE + dataSizeInBytes);
		//return new WavInfo(new FormatSpec(rate, channels == 2), dataSize);
	}
	
	/**
	 * Prepares the Short buffer for reading the wav file.
	 * @throws IOException
	 */
	private void initWavPcm() throws IOException {
		int shortBlocks = (int) Math.ceil(((float) dataSizeInBytes / 2) / blockSize) + 2;
		ByteBuffer buffer = ByteBuffer.allocate(shortBlocks * 2 * blockSize);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		wavStream.skip(HEADER_SIZE);
		wavStream.read(buffer.array(), 0, shortBlocks * 2 * blockSize);
		buffer.rewind();
		buffer.position(HEADER_SIZE);
		dataBuffer = buffer.asShortBuffer();
		dataBuffer.mark();
	}
	
	/**
	 * @return Short buffer with the data of the WAV file.
	 */
	public ShortBuffer getBuffer() {
		return dataBuffer;
	}
	
	/**
	 * resets the internal buffer.
	 */
	public void reset() {
		dataBuffer.reset();
	}
	
	/**
	 * Queries the Short buffer for more data.
	 * @param buffer
	 * @param offset
	 * @param size
	 */
	public void read(short[] buffer, int offset, int size) {
		//Log.i("getFromBuffer", "dst offset="+offset);
		//Log.i("getFromBuffer", "dst size="+size);
		//Log.i("getFromBuffer", "dst length="+buffer.length);
		//Log.i("getFromBuffer", "d===================");
		dataBuffer.get(buffer, offset, size);
	}
	
	
	private void checkFormat(Boolean b, String msg) throws IOException {
		if (!b)
			throw new IOException(msg);
	}


	/**
	 * Returns the sample rate.
	 * @return
	 */
	public int getSampleRate() {
		return sampleRate;
	}
	
	/**
	 * Returns the WAV size in Bytes.
	 * @return
	 */
	public int getDataSizeInBytes() {
		return dataSizeInBytes;
	}
	
	/**
	 * Returns the WAV size in Shorts.
	 * @return
	 */
	public int getDataSizeInShorts() {
		return dataSizeInBytes / 2;
	}
	
	/**
	 * 
	 */
	public short[] createBuffer() {
		return new short[blocks() * blockSize];
	}
	
	/**
	 * 
	 * @return
	 */
	public int blocks() {
		if (wavStream != null)
			return (int) Math.ceil((float) getDataSizeInShorts() / blockSize);
		return 0;
	}
	
	/**
	 * 
	 */
	public void scheduleDspCallback(DspCallback callback, long blockPeriodNanoseconds) {
		// schedule the DSP function.
		dspCallback = callback;
		dspHandle = scheduler.scheduleAtFixedRate(fileDspCallback,
				blockPeriodNanoseconds, blockPeriodNanoseconds,
				TimeUnit.NANOSECONDS);
	}
	

	/**
	 * Listener for when using AUDIO_SOURCE_FILE
	 */
	final Runnable fileDspCallback = new Runnable() {
		private long lastListenerStartTime = 0;

		public void run() {
			// Takes note of time between listeners
			long startTime = SystemClock.uptimeMillis();
			if (lastListenerStartTime != 0)
				callbackPeriod += (startTime - lastListenerStartTime);
			lastListenerStartTime = startTime;
			dspCallback.run();
		};
	};
	
	/**
	 * 
	 */
	public void readLoop(short[] buffer) {
		long times1, times2;
		reset();
		for (int block = 0; block < blocks(); block++) {
			readTicks++;
			// read from WAV buffer.
			times1 = SystemClock.uptimeMillis();
			read(buffer, block * blockSize, blockSize);
			times2 = SystemClock.uptimeMillis();
			sampleReadTime += (times2 - times1);
			// if (elapsedTime > (100000.0 * (float) blockSize /
			// sampleRate))
			// stopRunning();
		}
		// hang on untill DSP toggles.
		while (isRunning)
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Log.e("ERROR", "Thread was Interrupted");
			}
	}
	
	public void stopRunning() {
		isRunning = false;
		dspHandle.cancel(true);
	}


	@Override
	protected int getMinBufferSize() {
		return 4096;
	}

}
