import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BeatboxSampler extends JFrame {

    private static final int NUM_BUTTONS = 9;
    private final JButton[] padButtons = new JButton[NUM_BUTTONS];
    private final JLabel[] fileLabels = new JLabel[NUM_BUTTONS];
    
    private final List<File>[] padSampleLists = new ArrayList[NUM_BUTTONS];
    private final int[] currentSampleIndex = new int[NUM_BUTTONS];

    // Audio DSP Sliders
    private JSlider volumeSlider;
    private JSlider pitchSlider;
    private JSlider panSlider;
    private JSlider delaySlider;
    private JSlider tremoloSlider;
    
    private JLabel sampleInfoLabel;
    private int selectedPad = 0; 

    // Full 72 Sound Presets List grouped by Instruments (9 Notes / Positions each)
    private final String[] presetSoundNames = {
        // --- 1. Clean Piano (9 Notes) ---
        "Piano - C4", "Piano - D4", "Piano - E4", "Piano - F4", "Piano - G4", "Piano - A4", "Piano - B4", "Piano - C5", "Piano - D5",
        // --- 2. Classic Organ (9 Notes) ---
        "Organ - C4", "Organ - D4", "Organ - E4", "Organ - F4", "Organ - G4", "Organ - A4", "Organ - B4", "Organ - C5", "Organ - D5",
        // --- 3. Warm Pad Synth (9 Notes) ---
        "Pad Synth - C4", "Pad Synth - D4", "Pad Synth - E4", "Pad Synth - F4", "Pad Synth - G4", "Pad Synth - A4", "Pad Synth - B4", "Pad Synth - C5", "Pad Synth - D5",
        // --- 4. Retro Square Lead (9 Notes) ---
        "Square Lead - C4", "Square Lead - D4", "Square Lead - E4", "Square Lead - F4", "Square Lead - G4", "Square Lead - A4", "Square Lead - B4", "Square Lead - C5", "Square Lead - D5",
        // --- 5. Bright Brass Synth (9 Notes) ---
        "Brass - C4", "Brass - D4", "Brass - E4", "Brass - F4", "Brass - G4", "Brass - A4", "Brass - B4", "Brass - C5", "Brass - D5",
        // --- 6. Electric Bass (9 Notes) ---
        "Bass - C2", "Bass - D2", "Bass - E2", "Bass - F2", "Bass - G2", "Bass - A2", "Bass - B2", "Bass - C3", "Bass - D3",
        // --- 7. Percussion / Drums (9 Elements) ---
        "Drum - Kick", "Drum - Snare", "Drum - Hi-Hat Closed", "Drum - Hi-Hat Open", "Drum - Low Tom", "Drum - High Tom", "Drum - Clap", "Drum - Crash", "Drum - Ride",
        // --- 8. Sound Effects / FX (9 Elements) ---
        "FX - Laser Zap", "FX - 8-Bit Blip", "FX - Sub Drop", "FX - Noise Burst", "FX - Ambient Drone", "FX - Crystal Bell", "FX - Whistle", "FX - Alarm Siren", "FX - Space Sweep"
    };

    private final JComboBox<String> presetSoundComboBox = new JComboBox<>(presetSoundNames);
    private final int[] padPresetIndex = new int[NUM_BUTTONS]; // Selected preset index per pad

    private static class PadSettings {
        float volume = 1.0f; 
        float pitch = 1.0f;  
        float pan = 0.0f;    
        int delayMs = 0;     
        int tremoloDepth = 0;
    }
    private final PadSettings[] settings = new PadSettings[NUM_BUTTONS];

    public BeatboxSampler() {
        setTitle("Java Multi-Instrument Beatbox Sampler - HP_KON_JAR");
        setSize(1050, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        for (int i = 0; i < NUM_BUTTONS; i++) {
            settings[i] = new PadSettings();
            padSampleLists[i] = new ArrayList<>();
            currentSampleIndex[i] = 0;
            // Default preset on startup: Clean Piano scale (0 to 8 = C4 to D5)
            padPresetIndex[i] = i; 
        }

        initComponents();
        setupKeyBindings();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        sampleInfoLabel = new JLabel();
        sampleInfoLabel.setFont(new Font("Arial", Font.BOLD, 12));
        sampleInfoLabel.setForeground(new Color(30, 100, 200));

        // --- CENTRAL PANEL: 9 PADS ---
        JPanel padsPanel = new JPanel(new GridLayout(3, 3, 10, 10));
        padsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        int[] layoutOrder = {6, 7, 8, 3, 4, 5, 0, 1, 2}; 

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int gridPos = row * 3 + col;
                final int index = layoutOrder[gridPos];
                
                JPanel padContainer = new JPanel(new BorderLayout(2, 2));
                
                padButtons[index] = new JButton();
                padButtons[index].setFont(new Font("Arial", Font.PLAIN, 12));
                padButtons[index].setBackground(new Color(60, 63, 65));
                padButtons[index].setForeground(Color.WHITE);
                padButtons[index].setFocusPainted(false);

                padButtons[index].addActionListener(e -> {
                    selectedPad = index;
                    highlightSelectedPad();
                    playSound(index);
                    requestFocusInWindow();
                });

                padButtons[index].addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mousePressed(java.awt.event.MouseEvent e) {
                        selectedPad = index;
                        highlightSelectedPad();
                        if (SwingUtilities.isRightMouseButton(e)) {
                            loadWavFilesToPad(index);
                        }
                        requestFocusInWindow();
                    }
                });

                fileLabels[index] = new JLabel("", JLabel.CENTER);
                fileLabels[index].setFont(new Font("Arial", Font.PLAIN, 10));
                fileLabels[index].setForeground(Color.GRAY);

                padContainer.add(padButtons[index], BorderLayout.CENTER);
                padContainer.add(fileLabels[index], BorderLayout.SOUTH);
                padsPanel.add(padContainer);

                updatePadUI(index);
            }
        }

        add(padsPanel, BorderLayout.CENTER);

        // --- BOTTOM PANEL ---
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        JPanel sampleManagementPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        sampleManagementPanel.setBorder(BorderFactory.createTitledBorder("Pad Instrument & Note Assignment (RMB: Load WAV)"));

        JButton nextSampleBtn = new JButton("Next WAV Sample ➡");
        nextSampleBtn.addActionListener(e -> {
            if (!padSampleLists[selectedPad].isEmpty()) {
                currentSampleIndex[selectedPad] = (currentSampleIndex[selectedPad] + 1) % padSampleLists[selectedPad].size();
                updatePadUI(selectedPad);
                highlightSelectedPad();
            }
            requestFocusInWindow();
        });

        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        presetPanel.add(new JLabel("Instrument Note:"));
        presetSoundComboBox.setPreferredSize(new Dimension(190, 25));
        presetSoundComboBox.addActionListener(e -> {
            int selectedPreset = presetSoundComboBox.getSelectedIndex();
            if (selectedPreset != -1 && padPresetIndex[selectedPad] != selectedPreset) {
                padPresetIndex[selectedPad] = selectedPreset;
                updatePadUI(selectedPad);
            }
            requestFocusInWindow();
        });
        presetPanel.add(presetSoundComboBox);

        JButton clearPadBtn = new JButton("Reset to Preset");
        clearPadBtn.addActionListener(e -> {
            padSampleLists[selectedPad].clear();
            currentSampleIndex[selectedPad] = 0;
            updatePadUI(selectedPad);
            highlightSelectedPad();
            requestFocusInWindow();
        });

        sampleManagementPanel.add(nextSampleBtn);
        sampleManagementPanel.add(presetPanel);
        sampleManagementPanel.add(sampleInfoLabel);
        sampleManagementPanel.add(clearPadBtn);

        bottomPanel.add(sampleManagementPanel, BorderLayout.NORTH);

        // DSP Sliders
        JPanel controlPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        controlPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "DSP Real-Time Effects (WAV / Instrument Synths)",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 12), Color.DARK_GRAY
        ));

        volumeSlider = createSlider(0, 200, 100, val -> settings[selectedPad].volume = val / 100.0f);
        controlPanel.add(createControlWrapper("Volume", volumeSlider));

        pitchSlider = createSlider(50, 200, 100, val -> settings[selectedPad].pitch = val / 100.0f);
        controlPanel.add(createControlWrapper("Pitch / Speed", pitchSlider));

        panSlider = createSlider(-100, 100, 0, val -> settings[selectedPad].pan = val / 100.0f);
        controlPanel.add(createControlWrapper("Pan (L/R)", panSlider));

        delaySlider = createSlider(0, 500, 0, val -> settings[selectedPad].delayMs = val);
        controlPanel.add(createControlWrapper("Delay / Echo (ms)", delaySlider));

        tremoloSlider = createSlider(0, 100, 0, val -> settings[selectedPad].tremoloDepth = val);
        controlPanel.add(createControlWrapper("Modulation / Tremolo", tremoloSlider));

        JPanel signaturePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel sigLabel = new JLabel("Sampler HP_KON_JAR | Full Instrument Notes Engine");
        sigLabel.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 11));
        sigLabel.setForeground(Color.DARK_GRAY);
        signaturePanel.add(sigLabel);
        controlPanel.add(signaturePanel);

        bottomPanel.add(controlPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        highlightSelectedPad();
    }

    private JPanel createControlWrapper(String title, JSlider slider) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(" " + title + ":", JLabel.CENTER), BorderLayout.NORTH);
        panel.add(slider, BorderLayout.CENTER);
        return panel;
    }

    private void setupKeyBindings() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                int padIndex = -1;

                switch (keyCode) {
                    case KeyEvent.VK_NUMPAD1, KeyEvent.VK_1 -> padIndex = 0;
                    case KeyEvent.VK_NUMPAD2, KeyEvent.VK_2 -> padIndex = 1;
                    case KeyEvent.VK_NUMPAD3, KeyEvent.VK_3 -> padIndex = 2;
                    case KeyEvent.VK_NUMPAD4, KeyEvent.VK_4 -> padIndex = 3;
                    case KeyEvent.VK_NUMPAD5, KeyEvent.VK_5 -> padIndex = 4;
                    case KeyEvent.VK_NUMPAD6, KeyEvent.VK_6 -> padIndex = 5;
                    case KeyEvent.VK_NUMPAD7, KeyEvent.VK_7 -> padIndex = 6;
                    case KeyEvent.VK_NUMPAD8, KeyEvent.VK_8 -> padIndex = 7;
                    case KeyEvent.VK_NUMPAD9, KeyEvent.VK_9 -> padIndex = 8;
                }

                if (padIndex != -1) {
                    selectedPad = padIndex;
                    highlightSelectedPad();
                    playSound(padIndex);
                }
            }
        });
        setFocusable(true);
        requestFocusInWindow();
    }

    private JSlider createSlider(int min, int max, int init, java.util.function.Consumer<Integer> onChange) {
        JSlider slider = new JSlider(min, max, init);
        slider.setMajorTickSpacing((max - min) / 4);
        slider.setPaintTicks(true);
        slider.addChangeListener(e -> {
            onChange.accept(slider.getValue());
            requestFocusInWindow();
        });
        return slider;
    }

    private void highlightSelectedPad() {
        for (int i = 0; i < NUM_BUTTONS; i++) {
            if (i == selectedPad) {
                padButtons[i].setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
                volumeSlider.setValue((int)(settings[i].volume * 100));
                pitchSlider.setValue((int)(settings[i].pitch * 100));
                panSlider.setValue((int)(settings[i].pan * 100));
                delaySlider.setValue(settings[i].delayMs);
                tremoloSlider.setValue(settings[i].tremoloDepth);
                presetSoundComboBox.setSelectedIndex(padPresetIndex[i]);
            } else {
                padButtons[i].setBorder(null);
            }
        }
        updateSampleInfoDisplay();
    }

    private void updateSampleInfoDisplay() {
        if (sampleInfoLabel == null) return;
        List<File> list = padSampleLists[selectedPad];
        if (list.isEmpty()) {
            sampleInfoLabel.setText("Pad " + (selectedPad + 1) + " Active Sound: " + presetSoundNames[padPresetIndex[selectedPad]]);
        } else {
            File currentFile = list.get(currentSampleIndex[selectedPad]);
            sampleInfoLabel.setText("Pad " + (selectedPad + 1) + " | WAV Sample (" + (currentSampleIndex[selectedPad] + 1) + "/" + list.size() + "): " + currentFile.getName());
        }
    }

    private void loadWavFilesToPad(int index) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("WAV Audio Files", "wav"));
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            for (File f : files) {
                if (!padSampleLists[index].contains(f)) {
                    padSampleLists[index].add(f);
                }
            }
            updatePadUI(index);
        }
    }

    private void updatePadUI(int index) {
        List<File> list = padSampleLists[index];
        if (list.isEmpty()) {
            String presetShort = presetSoundNames[padPresetIndex[index]];
            if (presetShort.length() > 18) presetShort = presetShort.substring(0, 16) + "..";
            padButtons[index].setText("<html><center><b>[" + (index + 1) + "] PAD " + (index + 1) + "</b><br><font size='2' color='cyan'>" + presetShort + "</font></center></html>");
            fileLabels[index].setText("Preset Note/Sound");
            padButtons[index].setBackground(new Color(60, 63, 65));
        } else {
            File activeFile = list.get(currentSampleIndex[index]);
            String name = activeFile.getName();
            if (name.length() > 10) name = name.substring(0, 8) + "..";
            
            padButtons[index].setText("<html><center><b>[" + (index + 1) + "] PAD " + (index + 1) + "</b><br><font size='2' color='lightgreen'>" + name + " (" + list.size() + ")</font></center></html>");
            fileLabels[index].setText(list.size() + " WAV file(s)");
            padButtons[index].setBackground(new Color(40, 120, 80));
        }
        updateSampleInfoDisplay();
    }

    private void playSound(int index) {
        new Thread(() -> {
            try {
                AudioInputStream ais;
                List<File> list = padSampleLists[index];
                PadSettings pSet = settings[index];

                if (!list.isEmpty()) {
                    File file = list.get(currentSampleIndex[index]);
                    ais = AudioSystem.getAudioInputStream(file);
                    ais = processWavEffects(ais, pSet);
                } else {
                    ais = generatePresetToneStream(padPresetIndex[index], pSet);
                }

                Clip clip = AudioSystem.getClip();
                clip.open(ais);

                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB = (float) (20.0 * Math.log10(Math.max(pSet.volume, 0.0001)));
                    gainControl.setValue(Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB)));
                }

                if (clip.isControlSupported(FloatControl.Type.PAN)) {
                    FloatControl panControl = (FloatControl) clip.getControl(FloatControl.Type.PAN);
                    panControl.setValue(pSet.pan);
                }

                clip.start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private AudioInputStream processWavEffects(AudioInputStream originalAis, PadSettings pSet) throws IOException {
        AudioFormat format = originalAis.getFormat();
        byte[] audioBytes = originalAis.readAllBytes();
        originalAis.close();

        int sampleSize = format.getSampleSizeInBits() / 8;
        int channels = format.getChannels();

        int totalSamples = audioBytes.length / (sampleSize * channels);
        int newTotalSamples = (int) (totalSamples / pSet.pitch);
        byte[] pitchedBytes = new byte[newTotalSamples * sampleSize * channels];

        for (int i = 0; i < newTotalSamples; i++) {
            int srcIndex = (int) (i * pSet.pitch);
            if (srcIndex >= totalSamples) srcIndex = totalSamples - 1;

            for (int c = 0; c < channels; c++) {
                int srcByteIdx = (srcIndex * channels + c) * sampleSize;
                int destByteIdx = (i * channels + c) * sampleSize;
                for (int s = 0; s < sampleSize; s++) {
                    if (srcByteIdx + s < audioBytes.length && destByteIdx + s < pitchedBytes.length) {
                        pitchedBytes[destByteIdx + s] = audioBytes[srcByteIdx + s];
                    }
                }
            }
        }

        if (pSet.tremoloDepth > 0) {
            for (int i = 0; i < newTotalSamples; i++) {
                double mod = 1.0 - (pSet.tremoloDepth / 100.0) * 0.5 * (1 + Math.sin(i * 0.05));
                for (int c = 0; c < channels; c++) {
                    int idx = (i * channels + c) * sampleSize;
                    if (sampleSize == 2 && idx + 1 < pitchedBytes.length) {
                        int sample = (pitchedBytes[idx] & 0xFF) | (pitchedBytes[idx + 1] << 8);
                        sample = (int) (sample * mod);
                        pitchedBytes[idx] = (byte) (sample & 0xFF);
                        pitchedBytes[idx + 1] = (byte) ((sample >> 8) & 0xFF);
                    }
                }
            }
        }

        if (pSet.delayMs > 0) {
            int delaySamples = (int) ((format.getSampleRate() * pSet.delayMs) / 1000.0);
            int delayBytes = delaySamples * sampleSize * channels;
            byte[] delayedBytes = new byte[pitchedBytes.length + delayBytes];
            
            System.arraycopy(pitchedBytes, 0, delayedBytes, 0, pitchedBytes.length);
            
            for (int i = 0; i < pitchedBytes.length; i++) {
                int targetIdx = i + delayBytes;
                if (targetIdx < delayedBytes.length) {
                    int originalVal = delayedBytes[targetIdx];
                    int echoVal = (int) (pitchedBytes[i] * 0.5f);
                    delayedBytes[targetIdx] = (byte) Math.max(-128, Math.min(127, originalVal + echoVal));
                }
            }
            pitchedBytes = delayedBytes;
            newTotalSamples += delaySamples;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(pitchedBytes);
        return new AudioInputStream(bais, format, newTotalSamples);
    }

    // Advanced Sound Engine generating all 72 Presets / Instruments
    private AudioInputStream generatePresetToneStream(int presetIndex, PadSettings pSet) {
        float sampleRate = 44100.0f;
        float durationSeconds = 0.45f;
        int numSamples = (int) (sampleRate * durationSeconds);
        byte[] buffer = new byte[numSamples * 2];

        // Musical scale frequency lookup table (C4 to D5 & Bass C2 to D3)
        double[] noteFreqs = {
            261.63, 293.66, 329.63, 349.23, 392.00, 440.00, 493.88, 523.25, 587.33 // Octave 4/5
        };
        double[] bassFreqs = {
            65.41, 73.42, 82.41, 87.31, 98.00, 110.00, 123.47, 130.81, 146.83     // Octave 2/3
        };

        int category = presetIndex / 9;
        int notePos = presetIndex % 9;

        for (int i = 0; i < numSamples; i++) {
            double time = i / sampleRate;
            double envelope = Math.exp(-3.5 * time);
            double sampleVal = 0.0;

            switch (category) {
                case 0: // 1. Clean Piano (Harmonic Sine Engine)
                    double pFreq = noteFreqs[notePos] * pSet.pitch;
                    sampleVal = 0.7 * Math.sin(2 * Math.PI * pFreq * time)
                              + 0.2 * Math.sin(2 * Math.PI * pFreq * 2 * time)
                              + 0.1 * Math.sin(2 * Math.PI * pFreq * 3 * time);
                    envelope = Math.exp(-4.0 * time);
                    break;

                case 1: // 2. Classic Organ (Additive Harmonics)
                    double oFreq = noteFreqs[notePos] * pSet.pitch;
                    sampleVal = 0.5 * Math.sin(2 * Math.PI * oFreq * time)
                              + 0.3 * Math.sin(2 * Math.PI * oFreq * 2 * time)
                              + 0.2 * Math.sin(2 * Math.PI * oFreq * 4 * time);
                    envelope = Math.exp(-2.0 * time);
                    break;

                case 2: // 3. Warm Pad Synth (Soft Detuned Sine)
                    double padF = noteFreqs[notePos] * pSet.pitch;
                    sampleVal = 0.5 * Math.sin(2 * Math.PI * padF * time) 
                              + 0.5 * Math.sin(2 * Math.PI * (padF * 1.005) * time);
                    envelope = Math.exp(-1.8 * time);
                    break;

                case 3: // 4. Retro Square Lead
                    double sqF = noteFreqs[notePos] * pSet.pitch;
                    sampleVal = Math.sin(2 * Math.PI * sqF * time) >= 0 ? 0.7 : -0.7;
                    envelope = Math.exp(-3.0 * time);
                    break;

                case 4: // 5. Bright Brass Synth (Sawtooth Blend)
                    double brF = noteFreqs[notePos] * pSet.pitch;
                    double saw = 2.0 * ((time * brF) % 1.0) - 1.0;
                    sampleVal = saw * 0.7;
                    envelope = Math.exp(-3.2 * time);
                    break;

                case 5: // 6. Electric Bass (Deep Sub)
                    double bF = bassFreqs[notePos] * pSet.pitch;
                    sampleVal = Math.sin(2 * Math.PI * bF * time) + 0.3 * (2.0 * ((time * bF) % 1.0) - 1.0);
                    envelope = Math.exp(-3.8 * time);
                    break;

                case 6: // 7. Drums & Percussion Kit
                    switch (notePos) {
                        case 0 -> sampleVal = Math.sin(2 * Math.PI * 140.0 * Math.exp(-15.0 * time) * time); // Kick
                        case 1 -> sampleVal = (Math.random() * 2.0 - 1.0) * Math.exp(-12.0 * time); // Snare
                        case 2 -> sampleVal = (Math.random() * 2.0 - 1.0) * Math.exp(-30.0 * time); // HiHat Closed
                        case 3 -> sampleVal = (Math.random() * 2.0 - 1.0) * Math.exp(-10.0 * time); // HiHat Open
                        case 4 -> sampleVal = Math.sin(2 * Math.PI * 110.0 * time) * Math.exp(-8.0 * time); // Low Tom
                        case 5 -> sampleVal = Math.sin(2 * Math.PI * 220.0 * time) * Math.exp(-8.0 * time); // High Tom
                        case 6 -> sampleVal = (Math.random() * 2.0 - 1.0) * Math.exp(-18.0 * time); // Clap
                        case 7 -> sampleVal = (Math.random() * 2.0 - 1.0) * Math.exp(-4.0 * time);  // Crash
                        case 8 -> sampleVal = (Math.sin(2 * Math.PI * 800.0 * time) + Math.random()) * 0.5 * Math.exp(-6.0 * time); // Ride
                    }
                    break;

                case 7: // 8. Sound Effects / FX
                    switch (notePos) {
                        case 0 -> sampleVal = Math.sin(2 * Math.PI * (900.0 * Math.exp(-8.0 * time)) * time); // Laser Zap
                        case 1 -> sampleVal = Math.sin(2 * Math.PI * 650.0 * pSet.pitch * time) * (i % 1600 < 800 ? 1 : 0); // 8-Bit Blip
                        case 2 -> sampleVal = Math.sin(2 * Math.PI * (180.0 * Math.exp(-3.0 * time)) * time); // Sub Drop
                        case 3 -> sampleVal = (Math.random() * 2.0 - 1.0); // Noise Burst
                        case 4 -> sampleVal = (Math.sin(2 * Math.PI * 110.0 * time) + Math.sin(2 * Math.PI * 165.0 * time)) * 0.5; // Drone
                        case 5 -> sampleVal = Math.sin(2 * Math.PI * 523.25 * pSet.pitch * time) * Math.exp(-2.5 * time); // Crystal Bell
                        case 6 -> sampleVal = Math.sin(2 * Math.PI * (1200.0 + 300.0 * Math.sin(20.0 * time)) * time); // Whistle
                        case 7 -> sampleVal = Math.sin(2 * Math.PI * (400.0 + 200.0 * Math.sin(8.0 * time)) * time); // Alarm Siren
                        case 8 -> sampleVal = Math.sin(2 * Math.PI * (200.0 + 600.0 * time) * time); // Space Sweep
                    }
                    break;
            }

            if (pSet.tremoloDepth > 0) {
                envelope *= (1.0 - (pSet.tremoloDepth / 100.0) * 0.4 * (1 + Math.sin(i * 0.05)));
            }

            short sample = (short) (sampleVal * 32767 * envelope * 0.6);

            buffer[2 * i] = (byte) (sample & 0xff);
            buffer[2 * i + 1] = (byte) ((sample >> 8) & 0xff);
        }

        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        return new AudioInputStream(new ByteArrayInputStream(buffer), format, numSamples);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new BeatboxSampler().setVisible(true);
        });
    }
}