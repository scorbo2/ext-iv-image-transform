package ca.corbett.imageviewer.extensions.imagetransform;

import ca.corbett.extras.gradient.ColorSelectionType;
import ca.corbett.forms.Margins;
import ca.corbett.imageviewer.extensions.ImageViewerExtensionManager;
import ca.corbett.imageviewer.ui.MainWindow;
import ca.corbett.extras.image.ImageUtil;
import ca.corbett.extras.image.ImagePanel;
import ca.corbett.extras.image.ImagePanelConfig;
import ca.corbett.extras.MessageUtil;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.ColorField;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.PanelField;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A dialog that allows for basic transformation of an image: rotation by 90 degree increments,
 * or horizontal/vertical mirroring.
 *
 * @author scorbo2
 * @since ImageViewer 1.2
 */
public class TransformImageDialog extends JDialog implements KeyEventDispatcher {

    private MessageUtil messageUtil;
    private final File srcFile;
    private BufferedImage originalImage;
    private BufferedImage dBuffer;
    private ImagePanel imagePanel;
    private ColorField fillColorField;
    private boolean isDirty;

    private final MouseMotionListener fillColorSelectionMouseListener = new MouseMotionListener() {
        @Override
        public void mouseDragged(MouseEvent e) {
            // ignored
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            int mouseX = e.getX();
            int mouseY = e.getY();
            Point translatedPoint = imagePanel.getTranslatedPoint(new Point(mouseX, mouseY));
            if (translatedPoint.x < 0
                || translatedPoint.x >= dBuffer.getWidth()
                || translatedPoint.y < 0
                || translatedPoint.y >= dBuffer.getHeight()) {
                return;
            }
            int rgb = dBuffer.getRGB(translatedPoint.x, translatedPoint.y);
            fillColorField.setColor(new Color(rgb));
        }
    };

    private final MouseListener fillColorMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            imagePanel.removeMouseMotionListener(fillColorSelectionMouseListener);
            imagePanel.removeMouseListener(fillColorMouseListener);
        }
    };

    public TransformImageDialog(File file) {
        super(MainWindow.getInstance(), "Transform image");
        this.srcFile = file;
        setMinimumSize(new Dimension(500, 500));
        setSize(new Dimension(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width,
                              GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height));
        setResizable(true);
        setLocationRelativeTo(MainWindow.getInstance());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        initComponents();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            loadImage();
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
        }
    }

    /**
     * Overridden to handle memory cleanup.
     */
    @Override
    public void dispose() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
        if (originalImage != null) {
            getMessageUtil().getLogger().fine("TransformDialog: flushing image on dispose.");
            originalImage.flush();
            originalImage = null;
            dBuffer.flush();
            dBuffer = null;
        }
        super.dispose();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (!isActive()) {
            return false; // don't capture keystrokes if this dialog isn't showing.
        }

        if (e.getID() == KeyEvent.KEY_RELEASED) {
            switch (e.getKeyCode()) {

                case KeyEvent.VK_ESCAPE:
                    dispose();
                    break;

                case KeyEvent.VK_ENTER:
                    if (isDirty) {
                        if (JOptionPane.showConfirmDialog(this,
                                                          "Save current transform and close?\nThis will overwrite the original image.",
                                                          "Confirm",
                                                          JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                            saveTransform();
                            dispose();
                        }
                    }
                    else {
                        dispose();
                    }
                    break;
            }
        }

        else if (e.getID() == KeyEvent.KEY_PRESSED) {
            switch (e.getKeyCode()) {

                case KeyEvent.VK_Z:
                    if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) > 0) {
                        resetTransform();
                    }
                    break;

            }
        }

        return false;
    }

    private void saveTransform() {
        if (!isDirty) {
            getMessageUtil().getLogger().info("TranformDialog: no changes to save; closing.");
            dispose();
        }

        try {
            if (srcFile.exists()) {
                srcFile.delete();
            }

            if (isPng()) {
                Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("png");
                ImageWriter imageWriter = null;
                if (iter.hasNext()) {
                    imageWriter = iter.next();
                }
                if (imageWriter == null) {
                    throw new IOException("Unable to find PNG writer on this system.");
                }

                getMessageUtil().getLogger().log(Level.INFO, "TransformDialog: saving transformed jpeg image: {0}",
                                                 srcFile.getAbsolutePath());
                ImageUtil.saveImage(dBuffer, srcFile, imageWriter, null);
            }

            else if (isJpeg()) {
                getMessageUtil().getLogger().log(Level.INFO, "TransformDialog: saving transformed png image: {0}",
                                                 srcFile.getAbsolutePath());
                ImageUtil.saveImage(dBuffer, srcFile);
            }

            else {
                throw new IOException("Unsupported image format; must be png or jpeg image.");
            }

            // Force thumbnail regeneration for this image:
            ImageViewerExtensionManager.getInstance().removeThumbnail(srcFile);

            // Force reload of current image in MainWindow:
            MainWindow.getInstance().reloadCurrentImage();

            // We're done here:
            dispose();
        }
        catch (IOException ioe) {
            getMessageUtil().error("Problem transforming image: " + ioe.getMessage(), ioe);
        }

    }

    private boolean isPng() {
        return srcFile.getName().toLowerCase().endsWith("png");
    }

    private boolean isJpeg() {
        return srcFile.getName().toLowerCase().endsWith("jpg")
            || srcFile.getName().toLowerCase().endsWith("jpeg");
    }

    private void flipHorizontal() {
        BufferedImage newImage = new BufferedImage(dBuffer.getWidth(), dBuffer.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(dBuffer, dBuffer.getWidth(), 0, -dBuffer.getWidth(), dBuffer.getHeight(), null);
        g.dispose();
        isDirty = true;
        imagePanel.setImage(newImage);
        dBuffer.flush();
        dBuffer = newImage;
    }

    private void flipVertical() {
        BufferedImage newImage = new BufferedImage(dBuffer.getWidth(), dBuffer.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(dBuffer, 0, dBuffer.getHeight(), dBuffer.getWidth(), -dBuffer.getHeight(), null);
        g.dispose();
        isDirty = true;
        imagePanel.setImage(newImage);
        dBuffer.flush();
        dBuffer = newImage;
    }

    private void resetTransform() {
        if (isDirty) {
            if (JOptionPane.showConfirmDialog(this, "Revert all changes so far?",
                                              "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                return;
            }
        }
        if (dBuffer != null) {
            dBuffer.flush();
        }
        dBuffer = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dBuffer.createGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();
        imagePanel.setImage(dBuffer);
        isDirty = false;
    }

    private void rotateLeft() {
        // this is very very stupid but I don't understand AffineTransform at all, so...
        rotateRight();
        rotateRight();
        rotateRight();
    }

    private void rotateRight() {
        //AffineTransform transform = new AffineTransform();
        //transform.rotate(Math.toRadians(degrees));
        //g.drawImage(dBuffer, transform, null);
        BufferedImage newImage = new BufferedImage(dBuffer.getHeight(), dBuffer.getWidth(),
                                                   BufferedImage.TYPE_INT_RGB); // note swapping width/height
        Graphics2D g = newImage.createGraphics();
        AffineTransform transform = new AffineTransform(0.0, 1.0, -1.0, 0.0, dBuffer.getHeight(), 0.0);
        g.transform(transform);
        g.drawImage(dBuffer, 0, 0, dBuffer.getWidth(), dBuffer.getHeight(), null);
        g.dispose();
        isDirty = true;
        imagePanel.setImage(newImage);
        dBuffer.flush();
        dBuffer = newImage;
    }

    private void loadImage() {
        try {
            originalImage = ImageUtil.loadImage(srcFile);
            resetTransform();
        }
        catch (IOException | ArrayIndexOutOfBoundsException ioe) {
            getMessageUtil().error("Error loading image: " + ioe.getMessage(), ioe);
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(buildControlPanel(), BorderLayout.WEST);
        add(buildImagePanel(), BorderLayout.CENTER);
    }

    private FormPanel buildControlPanel() {
        FormPanel formPanel = new FormPanel();

        LabelField label = new LabelField("Transform:");
        label.setMargins(new Margins(5, 25, 0, 5, 0));
        label.setFont(LabelField.getDefaultFont().deriveFont(Font.BOLD, 16f));
        formPanel.add(label);

        PanelField panelField = new PanelField();
        panelField.setMargins(new Margins(0, 2, 0, 0, 0));
        JPanel p = panelField.getPanel();
        p.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton btn = new JButton("Rotate right");
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rotateRight();
            }

        });
        btn.setPreferredSize(new Dimension(200, 25));
        p.add(btn);
        formPanel.add(panelField);

        panelField = new PanelField();
        panelField.setMargins(new Margins(0, 0, 0, 0, 0));
        p = panelField.getPanel();
        p.setLayout(new FlowLayout(FlowLayout.CENTER));
        btn = new JButton("Rotate left");
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rotateLeft();
            }

        });
        btn.setPreferredSize(new Dimension(200, 25));
        p.add(btn);
        formPanel.add(panelField);

        panelField = new PanelField();
        panelField.setMargins(new Margins(0, 0, 0, 0, 0));
        p = panelField.getPanel();
        p.setLayout(new FlowLayout(FlowLayout.CENTER));
        btn = new JButton("Mirror vertical");
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                flipVertical();
            }

        });
        btn.setPreferredSize(new Dimension(200, 25));
        p.add(btn);
        formPanel.add(panelField);

        panelField = new PanelField();
        panelField.setMargins(new Margins(0, 0, 0, 0, 0));
        p = panelField.getPanel();
        p.setLayout(new FlowLayout(FlowLayout.CENTER));
        btn = new JButton("Mirror horizontal");
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                flipHorizontal();
            }

        });
        btn.setPreferredSize(new Dimension(200, 25));
        p.add(btn);
        formPanel.add(panelField);

        label = new LabelField("Rectangle fill:");
        label.setMargins(new Margins(5, 45, 0, 5, 0));
        label.setFont(LabelField.getDefaultFont().deriveFont(Font.BOLD, 16f));
        //TODO formPanel.addFormField(label);

        fillColorField = new ColorField("Fill color:", ColorSelectionType.SOLID).setColor(Color.BLACK);
        fillColorField.setMargins(new Margins(5, 25, 0, 5, 5));
        //TODO formPanel.addFormField(fillColorField);

        panelField = new PanelField();
        panelField.setMargins(new Margins(0, 0, 0, 0, 0));
        p = panelField.getPanel();
        btn = new JButton("Select from image");
        btn.setPreferredSize(new Dimension(200, 25));
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                imagePanel.addMouseMotionListener(fillColorSelectionMouseListener);
                imagePanel.addMouseListener(fillColorMouseListener);
            }

        });
        p.add(btn);
        //TODO formPanel.addFormField(panelField);

        panelField = new PanelField();
        panelField.setMargins(new Margins(0, 0, 0, 0, 0));
        p = panelField.getPanel();
        btn = new JButton("Fill rectangle");
        btn.setPreferredSize(new Dimension(200, 25));
        p.add(btn);
        //TODO formPanel.addFormField(panelField);

        panelField = new PanelField();
        panelField.setMargins(new Margins(0, 65, 0, 0, 0));
        p = panelField.getPanel();
        p.setLayout(new FlowLayout(FlowLayout.CENTER));
        btn = new JButton("Reset");
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetTransform();
            }

        });
        btn.setPreferredSize(new Dimension(200, 25));
        p.add(btn);
        formPanel.add(panelField);

        panelField = new PanelField();
        panelField.setMargins(new Margins(0, 0, 0, 0, 0));
        p = panelField.getPanel();
        p.setLayout(new FlowLayout(FlowLayout.CENTER));
        btn = new JButton("Cancel and close");
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }

        });
        btn.setPreferredSize(new Dimension(200, 25));
        p.add(btn);
        formPanel.add(panelField);

        panelField = new PanelField();
        panelField.setMargins(new Margins(0, 0, 0, 0, 0));
        p = panelField.getPanel();
        p.setLayout(new FlowLayout(FlowLayout.CENTER));
        btn = new JButton("Save and close");
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isDirty) {
                    getMessageUtil().info("No changes made.");
                }
                else {
                    saveTransform();
                }
            }

        });
        btn.setPreferredSize(new Dimension(200, 25));
        p.add(btn);
        formPanel.add(panelField);

        return formPanel;
    }

    private ImagePanel buildImagePanel() {
        ImagePanelConfig config = ImagePanelConfig.createSimpleReadOnlyProperties();
        imagePanel = new ImagePanel(config);
        return imagePanel;
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this, Logger.getLogger(TransformImageDialog.class.getName()));
        }
        return messageUtil;
    }

}
