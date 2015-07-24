/*
 *  What's That Color v0.3
 *
 *  What's That Color is an app to get the current pixel color
 *  at mouse pointer in RGB and HSB values 
 * 
 *  Developed By : deepak
 *  Email : deepakpk009@yahoo.in

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.deepak.wtc;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 *
 * @author deepak
 */
public class WhatsThatColor extends javax.swing.JFrame {

    // the point object that stores the current mouse pointer location
    private Point mouseLocation = null;
    // the robot object to get the screen pixel color at a paticular coordinate
    private Robot robot = null;
    // the pixel color
    private Color color = null;
    // the HSB color array 
    private float HSB[] = null;
    // the CMYK color values
    private float CMYK[] = null;
    // ICC Profile setting variables
    private ColorSpace colorSpace = null;
    // profile file array
    private File profiles[] = null;
    // profile file names
    private String profileNames[] = null;
    // default combobox model for combobox input data
    private DefaultComboBoxModel defaultComboBoxModel = null;
    // do processing flag which controls the color analysis process
    // this flag is specified as volatile as if not then the color analyser 
    // function wont work. specifies it may change from anyother part of the program. 
    private volatile boolean doProcessing = false;

    /**
     * Creates new form WhatsThatColor
     */
    public WhatsThatColor() {
        // load the profiles
        profiles = loadICCProfiles();
        // if profiles array equal to null or no of profiles equals to 0
        if (profiles == null || profiles.length == 0) {
            // add a single profile name
            profileNames = new String[1];
            // set the info string
            profileNames[0] = "No Profiles Present!";
        } else {
            // create profile names array with the profiles file array length
            profileNames = new String[profiles.length];
            // sequentially set the profile names to the profile name array
            String fileName;
            for (int i = 0; i < profiles.length; i++) {
                fileName = profiles[i].getName();
                // remove the extension from the file name
                profileNames[i] = fileName.substring(0, fileName.length() - 4);
            }
            // load the first profile
            try {
                colorSpace = new ICC_ColorSpace(
                        ICC_Profile.getInstance(
                        profiles[0].getAbsolutePath()));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Could NOT load profile!", "IOException", JOptionPane.ERROR_MESSAGE);
            }
        }
        // creat the default combobox model with the profile names
        defaultComboBoxModel = new DefaultComboBoxModel(profileNames);
        // set the profiles in auto generated code
        initComponents();

        // below is the code to add key listner 
        // for SPACE key typed event on the frame

        // get the frames content pane
        JPanel content = (JPanel) this.getContentPane();
        // create the key stroke as the SPACE key
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
        // register the keyboard action to the content pane
        content.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // if processing
                if (doProcessing) {
                    // stop processing
                    doProcessing = false;
                } else {
                    // start processing
                    doProcessing = true;
                }
            }
        }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

        try {
            // initilize the hsb color array with size 3 as for h,s and b.
            HSB = new float[3];
            // initilize the cmyk color array with size 4 as for c, m, y and k.
            CMYK = new float[4];
            // initilize the robot object
            robot = new Robot();
            // set processing as true
            doProcessing = true;
            // create and start the color analyser thread
            new ColorAnalyser().start();
        } catch (AWTException ex) {
            Logger.getLogger(WhatsThatColor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /*
     * method to load all ICC profiles from the profiles folder
     */
    private File[] loadICCProfiles() {
        // reference the profiles folder
        File f = new File("ICC_PROFILES");
        // if doesnt exists then make one and return null indication no profiles
        if (!f.exists()) {
            f.mkdir();
            return null;
        }
        // else return list of files with the profile files extension
        return f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                // only accept files with extension '.icc'
                return pathname.getName().toLowerCase().endsWith(".icc");
            }
        });
    }

    /*
     * the ColorAnalyser thread class
     * 
     * provides the continious pixel color analysis
     */
    private class ColorAnalyser extends Thread {

        @Override
        public void run() {
            // run forever
            while (true) {
                // process only if do processing is on/true
                if (doProcessing) {
                    // get the current mouse location on screen
                    mouseLocation = MouseInfo.getPointerInfo().getLocation();
                    // get the pixel color at that coordinate
                    color = robot.getPixelColor(mouseLocation.x, mouseLocation.y);
                    // set the color panel background color
                    colorPanel.setBackground(color);
                    // show the rgb color values
                    redTextField.setText(String.valueOf(color.getRed()));
                    greenTextField.setText(String.valueOf(color.getGreen()));
                    blueTextField.setText(String.valueOf(color.getBlue()));
                    // get the hsb color values from the rgb values
                    Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), HSB);
                    // show the hsb color values 
                    // hue in degrees
                    hueTextField.setText(String.valueOf(getDegree(HSB[0])));
                    // saturation in percentage
                    saturationTextField.setText(String.valueOf(getPercentage(HSB[1])));
                    // brightness in percentage
                    brightnessTextField.setText(String.valueOf(getPercentage(HSB[2])));
                    // show the hex color value
                    // using sub string as the alpha value is ignored ( 0x <ff> ff ff ff )
                    hexTextField.setText(Integer.toHexString(color.getRGB()).substring(2).toUpperCase());
                    // if color space is present then
                    if (colorSpace != null) {
                        // get the cmyk color based on selected ICC profiles.
                        color.getColorComponents(colorSpace, CMYK);
                        // set the CMYK values in percentage
                        cyanTextField.setText(String.valueOf(getPercentage(CMYK[0])));
                        magentaTextField.setText(String.valueOf(getPercentage(CMYK[1])));
                        yellowTextField.setText(String.valueOf(getPercentage(CMYK[2])));
                        keyTextField.setText(String.valueOf(getPercentage(CMYK[3])));
                    }
                }
            }
        }
    }

    /*
     * method to get the percentage value for an float input
     */
    private int getPercentage(float f) {
        return (int) (f * 100);
    }

    /*
     * method to get the degree value for an float input
     */
    private int getDegree(float f) {
        return (int) (f * 360);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        redLabel = new javax.swing.JLabel();
        greenLabel = new javax.swing.JLabel();
        blueLabel = new javax.swing.JLabel();
        hueLabel = new javax.swing.JLabel();
        saturationLabel = new javax.swing.JLabel();
        brightnessLabel = new javax.swing.JLabel();
        redTextField = new javax.swing.JTextField();
        greenTextField = new javax.swing.JTextField();
        blueTextField = new javax.swing.JTextField();
        hueTextField = new javax.swing.JTextField();
        saturationTextField = new javax.swing.JTextField();
        brightnessTextField = new javax.swing.JTextField();
        colorPanel = new javax.swing.JPanel();
        hexLabel = new javax.swing.JLabel();
        hexTextField = new javax.swing.JTextField();
        cyanLabel = new javax.swing.JLabel();
        magentaLabel = new javax.swing.JLabel();
        yellowLabel = new javax.swing.JLabel();
        keyLabel = new javax.swing.JLabel();
        cyanTextField = new javax.swing.JTextField();
        magentaTextField = new javax.swing.JTextField();
        yellowTextField = new javax.swing.JTextField();
        keyTextField = new javax.swing.JTextField();
        iccProfileComboBox = new javax.swing.JComboBox();
        iccProfileLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("What's That Color");
        setAlwaysOnTop(true);
        setResizable(false);

        redLabel.setText("Red");

        greenLabel.setText("Green");

        blueLabel.setText("Blue");

        hueLabel.setText("Hue");

        saturationLabel.setText("Saturation");

        brightnessLabel.setText("Brightness");

        redTextField.setEditable(false);

        greenTextField.setEditable(false);

        blueTextField.setEditable(false);

        hueTextField.setEditable(false);

        saturationTextField.setEditable(false);

        brightnessTextField.setEditable(false);

        colorPanel.setBackground(new java.awt.Color(0, 0, 0));

        javax.swing.GroupLayout colorPanelLayout = new javax.swing.GroupLayout(colorPanel);
        colorPanel.setLayout(colorPanelLayout);
        colorPanelLayout.setHorizontalGroup(
            colorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 165, Short.MAX_VALUE)
        );
        colorPanelLayout.setVerticalGroup(
            colorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 142, Short.MAX_VALUE)
        );

        hexLabel.setText("Hex");

        hexTextField.setEditable(false);

        cyanLabel.setText("Cyan");

        magentaLabel.setText("Magenta");

        yellowLabel.setText("Yellow");

        keyLabel.setText("Key");

        cyanTextField.setEditable(false);

        magentaTextField.setEditable(false);

        yellowTextField.setEditable(false);

        keyTextField.setEditable(false);

        iccProfileComboBox.setModel(defaultComboBoxModel);
        iccProfileComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                iccProfileComboBoxItemStateChanged(evt);
            }
        });

        iccProfileLabel.setText("ICC Profile");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(greenLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(redLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(blueLabel, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(blueTextField)
                            .addComponent(greenTextField)
                            .addComponent(redTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(brightnessLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(saturationLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(hueLabel, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(hueTextField)
                            .addComponent(saturationTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE)
                            .addComponent(brightnessTextField)))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(cyanLabel)
                            .addComponent(iccProfileLabel)
                            .addComponent(magentaLabel)
                            .addComponent(keyLabel)
                            .addComponent(yellowLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(iccProfileComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(cyanTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                                    .addComponent(magentaTextField)
                                    .addComponent(yellowTextField)
                                    .addComponent(keyTextField))
                                .addGap(18, 18, 18)
                                .addComponent(colorPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(hexLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(hexTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(18, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {blueTextField, greenTextField, redTextField});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cyanTextField, keyTextField, magentaTextField, yellowTextField});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {brightnessTextField, hueTextField, saturationTextField});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(redLabel)
                    .addComponent(hueLabel)
                    .addComponent(redTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hueTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(greenLabel)
                    .addComponent(saturationLabel)
                    .addComponent(greenTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(saturationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(blueLabel)
                    .addComponent(brightnessLabel)
                    .addComponent(blueTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(brightnessTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(iccProfileComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(iccProfileLabel))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cyanLabel)
                            .addComponent(cyanTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(magentaLabel)
                            .addComponent(magentaTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(yellowLabel)
                            .addComponent(yellowTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(keyLabel)
                            .addComponent(keyTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(colorPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(hexTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hexLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /*
     * method called when the user selects a icc profile from the combobox
     */
    private void iccProfileComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_iccProfileComboBoxItemStateChanged
        // TODO add your handling code here:
        // if profiles are present then
        if (profiles == null || profiles.length == 0) {
            try {
                // load the new profiles as selected in the combobox
                colorSpace = new ICC_ColorSpace(
                        ICC_Profile.getInstance(
                        profiles[iccProfileComboBox.getSelectedIndex()].getAbsolutePath()));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Could NOT load profile!", "IOException", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_iccProfileComboBoxItemStateChanged

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(WhatsThatColor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(WhatsThatColor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(WhatsThatColor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(WhatsThatColor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new WhatsThatColor().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel blueLabel;
    private javax.swing.JTextField blueTextField;
    private javax.swing.JLabel brightnessLabel;
    private javax.swing.JTextField brightnessTextField;
    private javax.swing.JPanel colorPanel;
    private javax.swing.JLabel cyanLabel;
    private javax.swing.JTextField cyanTextField;
    private javax.swing.JLabel greenLabel;
    private javax.swing.JTextField greenTextField;
    private javax.swing.JLabel hexLabel;
    private javax.swing.JTextField hexTextField;
    private javax.swing.JLabel hueLabel;
    private javax.swing.JTextField hueTextField;
    private javax.swing.JComboBox iccProfileComboBox;
    private javax.swing.JLabel iccProfileLabel;
    private javax.swing.JLabel keyLabel;
    private javax.swing.JTextField keyTextField;
    private javax.swing.JLabel magentaLabel;
    private javax.swing.JTextField magentaTextField;
    private javax.swing.JLabel redLabel;
    private javax.swing.JTextField redTextField;
    private javax.swing.JLabel saturationLabel;
    private javax.swing.JTextField saturationTextField;
    private javax.swing.JLabel yellowLabel;
    private javax.swing.JTextField yellowTextField;
    // End of variables declaration//GEN-END:variables
}
