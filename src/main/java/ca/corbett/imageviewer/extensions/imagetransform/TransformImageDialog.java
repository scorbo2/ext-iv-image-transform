package ca.corbett.imageviewer.extensions.imagetransform;

import ca.corbett.extras.MessageUtil;
import ca.corbett.extras.image.ImagePanel;
import ca.corbett.extras.image.ImagePanelConfig;
import ca.corbett.extras.image.ImageUtil;
import ca.corbett.extras.io.KeyStrokeManager;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.LabelField;
import ca.corbett.forms.fields.PanelField;
import ca.corbett.imageviewer.extensions.ImageViewerExtensionManager;
import ca.corbett.imageviewer.ui.MainWindow;
import ca.corbett.imageviewer.ui.ThumbCacheManager;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A dialog that allows for basic transformation of an image: rotation by 90 degree increments,
 * or horizontal/vertical mirroring.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since ImageViewer 1.2
 */
public class TransformImageDialog extends JDialog {

    private final KeyStrokeManager keyStrokeManager;
    private MessageUtil messageUtil;
    private final File srcFile;
    private BufferedImage originalImage;
    private BufferedImage dBuffer;
    private ImagePanel imagePanel;
    private boolean isDirty;

    public TransformImageDialog(File file) {
        super(MainWindow.getInstance(), "Transform image");
        this.srcFile = file;
        setMinimumSize(new Dimension(500, 500));
        setSize(new Dimension(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width,
                              GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height));
        setResizable(true);
        setLocationRelativeTo(MainWindow.getInstance());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setModal(true);
        initComponents();
        loadImage();
        keyStrokeManager = new KeyStrokeManager(this);
        configureKeyStrokes();
    }

    /**
     * Overridden to handle memory cleanup.
     */
    @Override
    public void dispose() {
        if (originalImage != null) {
            originalImage.flush();
        }
        if (dBuffer != null) {
            dBuffer.flush();
        }
        if (imagePanel != null) {
            imagePanel.dispose();
        }
        super.dispose();
    }

    private void confirmSaveAndDispose() {
        // If no changes have been made, just treat it like a cancel:
        if (!isDirty) {
            dispose();
            return;
        }

        // Otherwise, prompt for confirmation:
        if (getMessageUtil().askYesNo("Confirm",
                                      "Save current transform and close?\nThis will overwrite the original image.")
            == MessageUtil.YES) {
            saveTransform();
        }
    }

    private void saveTransform() {
        if (!isDirty) {
            getMessageUtil().getLogger().info("TransformImageDialog: no changes to save; closing.");
            dispose();
            return;
        }

        try {
            if (isPng()) {
                getMessageUtil().getLogger().log(Level.INFO, "TransformDialog: saving transformed png image: {0}",
                                                 srcFile.getAbsolutePath());
                if (srcFile.exists()) {
                    srcFile.delete();
                }
                ImageUtil.savePngImage(dBuffer, srcFile);
            }

            else if (isJpeg()) {
                getMessageUtil().getLogger().log(Level.INFO, "TransformDialog: saving transformed jpeg image: {0}",
                                                 srcFile.getAbsolutePath());
                if (srcFile.exists()) {
                    srcFile.delete();
                }
                ImageUtil.saveImage(dBuffer, srcFile);
            }

            else {
                throw new IOException("Unsupported image format; must be png or jpeg image.");
            }

            // Force thumbnail regeneration for this image:
            ThumbCacheManager.remove(srcFile);
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
        int imageType = originalImage.getColorModel().hasAlpha()
            ? BufferedImage.TYPE_INT_ARGB
            : BufferedImage.TYPE_INT_RGB;
        BufferedImage newImage = new BufferedImage(dBuffer.getWidth(), dBuffer.getHeight(), imageType);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(dBuffer, dBuffer.getWidth(), 0, -dBuffer.getWidth(), dBuffer.getHeight(), null);
        g.dispose();
        isDirty = true;
        imagePanel.setImage(newImage);
        dBuffer.flush();
        dBuffer = newImage;
    }

    private void flipVertical() {
        int imageType = originalImage.getColorModel().hasAlpha()
            ? BufferedImage.TYPE_INT_ARGB
            : BufferedImage.TYPE_INT_RGB;
        BufferedImage newImage = new BufferedImage(dBuffer.getWidth(), dBuffer.getHeight(), imageType);
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
        int imageType = originalImage.getColorModel().hasAlpha()
            ? BufferedImage.TYPE_INT_ARGB
            : BufferedImage.TYPE_INT_RGB;
        dBuffer = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), imageType);
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
        int imageType = originalImage.getColorModel().hasAlpha()
            ? BufferedImage.TYPE_INT_ARGB
            : BufferedImage.TYPE_INT_RGB;
        BufferedImage newImage = new BufferedImage(dBuffer.getHeight(), dBuffer.getWidth(),
                                                   imageType); // note swapping width/height
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
        formPanel.setBorderMargin(10);
        formPanel.add(LabelField.createBoldHeaderLabel("Transform:", 16));

        PanelField panelField = new PanelField(new GridBagLayout());
        panelField.setShouldExpand(true);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(4, 0, 4, 0);
        JPanel panel = panelField.getPanel();
        panel.add(createButton("Rotate right", e -> rotateRight()), gbc);
        gbc.gridy++;
        panel.add(createButton("Rotate left", e -> rotateLeft()), gbc);
        gbc.gridy++;
        panel.add(createButton("Mirror vertical", e -> flipVertical()), gbc);
        gbc.gridy++;
        panel.add(createButton("Mirror horizontal", e -> flipHorizontal()), gbc);
        gbc.gridy++;

        gbc.insets = new Insets(30, 0, 4, 0);
        panel.add(createButton("Reset transform", e -> resetTransform()), gbc);
        gbc.gridy++;
        gbc.insets = new Insets(4, 0, 4, 0);
        panel.add(createButton("Cancel and close", e -> dispose()), gbc);
        gbc.gridy++;
        panel.add(createButton("Save and close", e -> saveTransform()), gbc);
        formPanel.add(panelField);

        return formPanel;
    }

    private JButton createButton(String text, ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(180, 24));
        btn.setMinimumSize(new Dimension(180, 24));
        btn.addActionListener(listener);
        return btn;
    }

    private ImagePanel buildImagePanel() {
        ImagePanelConfig config = ImagePanelConfig.createSimpleReadOnlyProperties();
        imagePanel = new ImagePanel(config);
        return imagePanel;
    }

    /**
     * Hit enter to save and close, escape to cancel and close, or ctrl+z to reset transform (undo all).
     */
    private void configureKeyStrokes() {
        keyStrokeManager.registerHandler("ESC", e -> dispose());
        keyStrokeManager.registerHandler("ENTER", e -> confirmSaveAndDispose());
        keyStrokeManager.registerHandler("Ctrl+Z", e -> resetTransform());
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this, Logger.getLogger(TransformImageDialog.class.getName()));
        }
        return messageUtil;
    }
}
