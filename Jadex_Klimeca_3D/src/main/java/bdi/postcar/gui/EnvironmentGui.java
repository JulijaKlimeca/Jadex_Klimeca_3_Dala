package bdi.postcar.gui;

import bdi.postcar.environment.ILocation;
import bdi.postcar.environment.impl.*;
import jadex.bridge.ComponentIdentifier;
import jadex.commons.gui.SGUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.MalformedURLException;

public class EnvironmentGui extends JFrame {
    private static final UIDefaults icons;
    private static final String pathToImages = "src/main/java/bdi/postcar/gui/images/";

    static {
        try {
            icons = new UIDefaults(new Object[]
                    {
                            "car", new ImageIcon(new File(pathToImages + "drone.png").toURL()),
                            "background", new ImageIcon(new File(pathToImages + "background.png").toURL()),
                    });
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private Timer timer;

    public EnvironmentGui() {
        super("Drone Environment");

        final Environment env = Environment.getInstance();

        final Image car_image = ((ImageIcon) icons.getIcon("car")).getImage();
        final Image background_image = ((ImageIcon) icons.getIcon("background")).getImage();

        final JLabel car = new JLabel("dummy", new ImageIcon(car_image), JLabel.CENTER);
        car.setVerticalTextPosition(JLabel.CENTER);
        car.setHorizontalTextPosition(JLabel.RIGHT);

        final JPanel map = new JPanel() {

            protected void paintComponent(Graphics g) {

                Rectangle bounds = getBounds();

                Image image = background_image;
                int w = image.getWidth(this);
                int h = image.getHeight(this);
                if (w > 0 && h > 0) {
                    for (int y = 0; y < bounds.height; y += h) {
                        for (int x = 0; x < bounds.width; x += w) {
                            g.drawImage(image, x, y, this);
                        }
                    }
                }

                Car[] cars = env.getCars();
                for (Car value : cars) {
                    int colorcode = Math.abs(ComponentIdentifier.getPlatformPrefix(value.getAgentIdentifier().getParent().getLocalName()).hashCode() % 8);
                    Point p = onScreenLocation(value.getLocation(), bounds);
                    w = (int) (value.getVisionRange() * bounds.width);
                    h = (int) (value.getVisionRange() * bounds.height);
                    g.setColor(new Color(240, 249, 180, 150));
                    g.fillOval(p.x - w, p.y - h, w * 2, h * 2);
                }

                //Uzzimet agentu
                for (int i = 0; i < cars.length; i++) {
                    Point p = onScreenLocation(cars[i].getLocation(), bounds);
                    car.setText("<html>"
                            + "Drone" +"<br>"
                            + "battery: " + (int) (cars[i].getChargestate() * 100.0) + "%<br>");
                    render(g, car, new Point(p.x + 45, p.y));
                }
            }
        };

        map.addComponentListener(new ComponentAdapter() {
            private Rectangle _bounds;
            public void componentResized(ComponentEvent ce) {
                Rectangle bounds = map.getBounds();
                if (_bounds == null) _bounds = bounds;
                double scale = Math.min(bounds.width / (double) _bounds.width,
                        bounds.height / (double) _bounds.height);

                // agents
                ((ImageIcon) car.getIcon()).setImage(
                        car_image.getScaledInstance(
                                (int) (car_image.getWidth(map) * scale),
                                (int) (car_image.getHeight(map) * scale),
                                Image.SCALE_DEFAULT));
            }
        });

        // Vide
        getContentPane().add(BorderLayout.CENTER, map);
        setSize(600, 600);
        setLocation(SGUI.calculateMiddlePosition(EnvironmentGui.this));
        setVisible(true);

        // atjauninat katras 50 milisekundes
        timer = new Timer(50, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                map.invalidate();
                map.repaint();
            }
        });
        timer.start();

        //iziet
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    private Point onScreenLocation(ILocation loc, Rectangle bounds) {
        return new Point((int) (bounds.width * loc.getX()),
                (int) (bounds.height * (1.0 - loc.getY())));
    }

    private void render(Graphics g, Component comp, Point p) {
        Dimension d = comp.getPreferredSize();
        Rectangle bounds = new Rectangle(p.x - d.width / 2, p.y - d.height / 2, d.width + 1, d.height);
        comp.setBounds(bounds);
        g.translate(bounds.x, bounds.y);
        comp.paint(g);
        g.translate(-bounds.x, -bounds.y);
    }
}

