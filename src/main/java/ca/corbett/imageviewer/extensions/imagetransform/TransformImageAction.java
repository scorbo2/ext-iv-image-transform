package ca.corbett.imageviewer.extensions.imagetransform;

import ca.corbett.extras.EnhancedAction;
import ca.corbett.imageviewer.ui.ImageInstance;
import ca.corbett.imageviewer.ui.MainWindow;

import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Launches the TransformImageDialog for the selected image, if there is one.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class TransformImageAction extends EnhancedAction {

    private static final String NAME = "Transform image...";
    private static TransformImageAction instance;

    private TransformImageAction() {
        super(NAME);
    }

    public static TransformImageAction getInstance() {
        if (instance == null) {
            instance = new TransformImageAction();
        }
        return instance;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ImageInstance currentImage = MainWindow.getInstance().getSelectedImage();
        if (currentImage.isEmpty()) {
            MainWindow.getInstance().showMessageDialog(NAME, "Nothing selected.");
            return;
        }

        // Ensure correct file format:
        File file = currentImage.getImageFile();
        if (!TransformImageExtension.fileExtensionIsSupported(file)) {
            MainWindow.getInstance().showMessageDialog(NAME,
                                                       "Image transformation can currently only be performed on jpeg or png images.");
            return;
        }

        new TransformImageDialog(file).setVisible(true);
    }
}
