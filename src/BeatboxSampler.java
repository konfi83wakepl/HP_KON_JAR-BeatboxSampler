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

    // Advanced parameter sliders
    private JSlider volumeSlider;
    private JSlider pitchSlider;
    private JSlider panSlider;
    private JSlider delaySlider;
    private JSlider tremoloSlider;
    
    private JLabel sampleInfoLabel;
    private int selectedPad = 0; 

    // Built-in synthesized sound presets (16 sounds / notes)
    private final String[] presetSoundNames = {
        "1. Note C4 (Sine)", "2. Note D4 (Sine)", "3. Note E4 (Sine)", "4. Note F4 (Sine)",
        "5. Note G4 (Sine)", "6. Note A4 (Sine)", "7. Note B4 (Sine)", "8. Note C5 (Sine)",
        "9. Kick Drum Synth", "10. Snare Synth", "11. Hi-Hat Synth", "12. Sub Bass 100Hz",
        "13. Laser Zap", "14. Organ C4", "15. Bell Tone", "16. Detuned Saw"
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
        setTitle("Java Advanced Beatbox Multi-Sampler & WAV DSP Engine - HP_KON_JAR");
        setSize(1050, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        for (int i = 0; i < NUM_BUTTONS; i++) {
            settings[i] = new PadSettings();
            padSampleLists[i] = new ArrayList<>();
            currentSampleIndex[i] = 0;
            padPresetIndex[i] = i; 
        }

        initComponents();
        setupKeyBindings();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // --- NAJPIERW TWORZYMY ETYKIETĘ INFORMACYJNĄ (aby uniknąć NullPointerException) ---
        sampleInfoLabel = new JLabel();
        sampleInfoLabel.setFont(new Font("Arial", Font.BOLD, 12));
        sampleInfoLabel.setForeground(new Color(30, 100, 200));

        // --- PANEL CENTRALNY: 9 PADÓW ---
        JPanel padsPanel = new JPanel(new GridLayout(3, 3, 10, 10));
        padsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        int[] layoutOrder = {6, 7, 8, 3, 4, 5, 0, 1, 2}; 

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int gridPos = row * 3 + col;
                final int index = layoutOrder[gridPos];
                
                JPanel padContainer = new JPanel(new BorderLayout(2, 2));
                
                int numpadKeyVal = index + 1; 
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

        // --- PANEL DOLNY: ZARZĄDZANIE PRÓBKAMI I EFEKTY DSP ---
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        JPanel sampleManagementPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        sampleManagementPanel.setBorder(BorderFactory.createTitledBorder("Pad Management & Synth Sound Selection (RMB: Load WAV)"));

        JButton nextSampleBtn = new JButton("Next WAV Sample ➡");
        nextSampleBtn.addActionListener(e -> {
            if (!padSampleLists[selectedPad].isEmpty()) {
                currentSampleIndex[selectedPad] = (currentSampleIndex[selectedPad] + 1) % padSampleLists[selectedPad].size();
                updatePadUI(selectedPad);
                highlightSelectedPad();
            }
            requestFocusInWindow();
        });

        // Combo box for 16 sounds selection
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        presetPanel.add(new JLabel("Change Sound/Note:"));
        presetSoundComboBox.setPreferredSize(new Dimension(180, 25));
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

        // DSP Effects Sliders Panel
        JPanel controlPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        controlPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "Advanced DSP Effects & Parameters (WAV / Synth)",
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

        // Footer signature label
        JPanel signaturePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel sigLabel = new JLabel("Sampler HP_KON_JAR | All effects apply to both WAV & Synths!");
        sigLabel.setFont(new Font("Arial", Font.ITALIC, 11));
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
            sampleInfoLabel.setText("Pad " + (selectedPad + 1) + " Active Synth: " + presetSoundNames[padPresetIndex[selectedPad]]);
        } else {
            File currentFile = list.get(currentSampleIndex[selectedPad]);
            sampleInfoLabel.setText("Pad " + (selectedPad + 1) + " | WAV Sample " + (currentSampleIndex[selectedPad] + 1) + "/" + list.size() + ": " + currentFile.getName());
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
            fileLabels[index].setText("Preset Note/Synth");
            padButtons[index].setBackground(new Color(60, 63, 65));
        } else {
            File activeFile = list.get(currentSampleIndex[index]);
            String name = activeFile.getName();
            if (name.length() > 10) name = name.substring(0, 8) + "..";
            
            padButtons[index].setText("<html><center><b>[" + (index + 1) + "] PAD " + (index + 1) + "</b><br><font size='2' color='lightgreen'>" + name + " (" + list.size() + ")</font></center></html>");
            fileLabels[index].setText(list.size() + " samples (Act: " + (currentSampleIndex[index] + 1) + ")");
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

    private AudioInputStream generatePresetToneStream(int presetIndex, PadSettings pSet) {
        float sampleRate = 44100.0f;
        float durationSeconds = 0.4f;
        int numSamples = (int) (sampleRate * durationSeconds);
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            double time = i / sampleRate;
            double envelope = 1.0 - ((double) i / numSamples);
            double sampleVal = 0.0;

            switch (presetIndex) {
                case 0: // Note C4 (261.63 Hz)
                    sampleVal = Math.sin(2 * Math.PI * 261.63 * pSet.pitch * time);
                    break;
                case 1: // Note D4 (293.66 Hz)
                    sampleVal = Math.sin(2 * Math.PI * 293.66 * pSet.pitch * time);
                    break;
                case 2: // Note E4 (329.63 Hz)
                    sampleVal = Math.sin(2 * Math.PI * 329.63 * pSet.pitch * time);
                    break;
                case 3: // Note F4 (349.23 Hz)
                    sampleVal = Math.sin(2 * Math.PI * 349.23 * pSet.pitch * time);
                    break;
                case 4: // Note G4 (392.00 Hz)
                    sampleVal = Math.sin(2 * Math.PI * 392.00 * pSet.pitch * time);
                    break;
                case 5: // Note A4 (440.00 Hz)
                    sampleVal = Math.sin(2 * Math.PI * 440.00 * pSet.pitch * time);
                    break;
                case 6: // Note B4 (493.88 Hz)
                    sampleVal = Math.sin(2 * Math.PI * 493.88 * pSet.pitch * time);
                    break;
                case 7: // Note C5 (523.25 Hz)
                    sampleVal = Math.sin(2 * Math.PI * 523.25 * pSet.pitch * time);
                    break;
                case 8: // Kick Drum Synth
                    double kickFreq = 150.0 * Math.max(0.1, (1.0 - time * 4.0));
                    sampleVal = Math.sin(2 * Math.PI * kickFreq * time) * Math.max(0, 1.0 - time * 3.0);
                    break;
                case 9: // Snare Synth
                    double noise = Math.random() * 2.0 - 1.0;
                    sampleVal = noise * Math.max(0, 1.0 - time * 5.0);
                    break;
                case 10: // Hi-Hat Synth
                    double hhNoise = Math.random() * 2.0 - 1.0;
                    sampleVal = hhNoise * Math.max(0, 1.0 - time * 12.0);
                    break;
                case 11: // Sub Bass 100Hz
                    sampleVal = Math.sin(2 * Math.PI * 100.0 * pSet.pitch * time);
                    envelope *= 1.2;
                    break;
                case 12: // Laser Zap
                    double zapFreq = 800.0 * Math.max(0.0, (1.0 - time * 5.0));
                    sampleVal = Math.sin(2 * Math.PI * zapFreq * time);
                    break;
                case 13: // Organ C4
                    double orgFreq = 261.63 * pSet.pitch;
                    sampleVal = Math.sin(2 * Math.PI * orgFreq * time) + 0.5 * Math.sin(2 * Math.PI * orgFreq * 2 * time);
                    break;
                case 14: // Bell Tone
                    double bellFreq = 523.25 * pSet.pitch;
                    sampleVal = Math.sin(2 * Math.PI * bellFreq * time) * Math.exp(-4.0 * time);
                    break;
                case 15: // Detuned Saw
                    double f = 220.0 * pSet.pitch;
                    sampleVal = (2.0 * ((time * f) % 1.0) - 1.0) + (2.0 * ((time * (f * 1.01)) % 1.0) - 1.0);
                    sampleVal *= 0.5;
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