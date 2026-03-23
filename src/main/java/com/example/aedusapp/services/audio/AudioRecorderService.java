package com.example.aedusapp.services.audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class AudioRecorderService {
    private TargetDataLine targetLine;
    private File audioFile;
    private Thread recordThread;
    private volatile boolean recording = false;

    public void startRecording() {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                 System.out.println("Microphone not supported");
                 return;
            }

            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(format);
            targetLine.start();

            recording = true;
            File dir = new File("uploads/audio");
            if (!dir.exists()) dir.mkdirs();
            
            audioFile = new File(dir, "audio_msg_" + System.currentTimeMillis() + ".wav");

            recordThread = new Thread(() -> {
                try (AudioInputStream ais = new AudioInputStream(targetLine)) {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, audioFile);
                } catch (IOException e) {
                    if (recording) e.printStackTrace(); 
                }
            });
            recordThread.start();

        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }

    public File stopRecording() {
        recording = false;
        if (targetLine != null) {
            targetLine.stop();
            targetLine.close();
        }
        if (recordThread != null) {
            try {
                recordThread.join(2000); // Allow more time for flushing
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return audioFile;
    }

    public void playAudio(String filePath, Runnable onStart, Runnable onEnd) {
        if (filePath == null || filePath.isEmpty()) {
            System.err.println("No audio path provided for playback.");
            return;
        }
        new Thread(() -> {
            AudioInputStream audioStream = null;
            Clip clip = null;
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    file = new File(System.getProperty("user.dir"), filePath);
                }

                if (!file.exists()) {
                    System.err.println("Audio file NOT found: " + filePath);
                    return;
                }
                
                System.out.println("Attempting to play: " + file.getAbsolutePath());
                audioStream = AudioSystem.getAudioInputStream(file);
                clip = AudioSystem.getClip();
                
                final AudioInputStream finalStream = audioStream;
                final Clip finalClip = clip;
                final Runnable finalOnEnd = onEnd;
                
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        try {
                            finalClip.close();
                            finalStream.close();
                            System.out.println("Playback finished and resources released.");
                            if (finalOnEnd != null) finalOnEnd.run();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

                clip.open(audioStream);
                if (onStart != null) onStart.run();
                clip.start();
                
            } catch (Exception e) {
                System.err.println("Playback ERROR for file " + filePath + ": " + e.getMessage());
                e.printStackTrace();
                if (onEnd != null) onEnd.run();
                try {
                    if (clip != null) clip.close();
                    if (audioStream != null) audioStream.close();
                } catch (Exception ignored) {}
            }
        }).start();
    }
}
