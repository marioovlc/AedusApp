package com.example.aedusapp.services.audio;

import com.example.aedusapp.utils.ConcurrencyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioRecorderService {
    private static final Logger logger = LoggerFactory.getLogger(AudioRecorderService.class);
    private TargetDataLine targetLine;
    private File audioFile;
    private final AtomicBoolean recording = new AtomicBoolean(false);

    /**
     * Interface to receive real-time updates from the recorder.
     */
    public interface RecordingListener {
        void onAudioLevel(double level); // Level from 0.0 to 1.0
    }

    public void startRecording(RecordingListener listener) {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                 logger.error("Microphone line not supported.");
                 return;
            }

            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(format);
            targetLine.start();

            recording.set(true);
            File dir = new File("uploads/audio");
            if (!dir.exists()) dir.mkdirs();
            
            audioFile = new File(dir, "audio_msg_" + System.currentTimeMillis() + ".wav");

            ConcurrencyManager.submit(() -> {
                try (AudioInputStream ais = new AudioInputStream(targetLine)) {
                    byte[] buffer = new byte[1024];
                    logger.info("Recording to file: {}", audioFile.getAbsolutePath());
                    
                    // We need a specific loop to calculate levels if listener is provided
                    if (listener != null) {
                        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                            while (recording.get()) {
                                int count = targetLine.read(buffer, 0, buffer.length);
                                if (count > 0) {
                                    out.write(buffer, 0, count);
                                    double peak = calculatePeak(buffer, count);
                                    listener.onAudioLevel(peak);
                                }
                            }
                            // Save the collected audio to file
                            try (AudioInputStream finalStream = new AudioInputStream(
                                    new java.io.ByteArrayInputStream(out.toByteArray()), format, out.size() / format.getFrameSize())) {
                                AudioSystem.write(finalStream, AudioFileFormat.Type.WAVE, audioFile);
                            }
                        }
                    } else {
                        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, audioFile);
                    }
                } catch (IOException e) {
                    if (recording.get()) {
                        logger.error("IO Error during recording: {}", e.getMessage(), e);
                    }
                }
            });

        } catch (LineUnavailableException ex) {
            logger.error("Microphone line unavailable: {}", ex.getMessage(), ex);
        }
    }

    private double calculatePeak(byte[] buffer, int count) {
        int max = 0;
        for (int i = 0; i < count; i += 2) {
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xff));
            max = Math.max(max, Math.abs(sample));
        }
        return Math.min(1.0, (double) max / Short.MAX_VALUE);
    }

    public File stopRecording() {
        if (!recording.get()) return null;
        recording.set(false);
        if (targetLine != null) {
            targetLine.stop();
            targetLine.close();
            logger.info("Recording stopped.");
        }
        return audioFile;
    }

    public void playAudio(String filePath, Runnable onStart, Runnable onEnd) {
        if (filePath == null || filePath.isEmpty()) {
            logger.warn("No audio path provided for playback.");
            return;
        }

        ConcurrencyManager.submit(() -> {
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    file = new File(System.getProperty("user.dir"), filePath);
                }

                if (!file.exists()) {
                    logger.error("Audio file NOT found: {}", filePath);
                    if (onEnd != null) onEnd.run();
                    return;
                }
                
                logger.info("Playing audio: {}", file.getAbsolutePath());
                try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
                     Clip clip = AudioSystem.getClip()) {
                    
                    clip.addLineListener(event -> {
                        if (event.getType() == LineEvent.Type.STOP) {
                            logger.debug("Playback finished.");
                            if (onEnd != null) onEnd.run();
                        }
                    });

                    clip.open(audioStream);
                    if (onStart != null) onStart.run();
                    clip.start();
                    
                    // Wait until playback finished
                    Thread.sleep(clip.getMicrosecondLength() / 1000 + 100);
                }
            } catch (Exception e) {
                logger.error("Playback ERROR for file {}: {}", filePath, e.getMessage());
                if (onEnd != null) onEnd.run();
            }
        });
    }
}
