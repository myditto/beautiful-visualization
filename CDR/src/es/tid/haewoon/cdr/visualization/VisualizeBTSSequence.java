package es.tid.haewoon.cdr.visualization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import processing.core.PApplet;
import processing.core.PFont;
import toxi.geom.Polygon2D;
import toxi.geom.Vec2D;
import toxi.geom.mesh2d.Voronoi;
import toxi.processing.ToxiclibsSupport;
import codeanticode.glgraphics.GLConstants;
import de.fhpotsdam.unfolding.Map;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.utils.MapUtils;
import es.tid.haewoon.cdr.analysis.FindSequences;
import es.tid.haewoon.cdr.util.CDRUtil;
import es.tid.haewoon.cdr.util.Cell;
import es.tid.haewoon.cdr.util.Constants;
import es.tid.haewoon.cdr.util.RankComparator;
import es.tid.haewoon.cdr.util.Transition;

public class VisualizeBTSSequence extends PApplet {
    private static Logger logger = Logger.getLogger(VisualizeBTSSequence.class);

    private static final long serialVersionUID = -4085039003253887831L;
    java.util.Map<String, Location> BTS2Location = new HashMap<String, Location>();
    java.util.Map<Integer, List<String>> color2seq = new HashMap<Integer, List<String>>();
    int fileIndex = 0;
    List<File> files;
    PFont font;
    int from = 0xFFe9b20e;
    ToxiclibsSupport gfx;
    Map map;

    double max_weight = -1;

    boolean normalize = false;
    Random random = new Random(0);

    boolean saveFile = false;
    boolean showAgg = false;
    boolean showHome = false;
    boolean showLabel = false;
    boolean showSeq = false;
    boolean showVoronoi = false;
    String targetPath;
    
    java.util.Map<String, String> num2home = new HashMap<String, String>();
    java.util.Map<String, String> num2work = new HashMap<String, String>();
    

    int to = 0xFFef1628;
    java.util.Map<Transition, Double> tran2wt = new HashMap<Transition, Double>();

    Voronoi voronoi;

    public void draw() {
        map.draw();
        interactiveMode();
    }

    public void interactiveMode() {
        fill(100);
        textSize(14);
        int yLoc = 30;
        text("Press V to toggle the Voronoi diagram", 15, yLoc);
        // text("Press S to save the current screen", 15, 50);
        yLoc += 20;
        text("Press Q to toggle the individual sequence", 15, yLoc);
        yLoc += 20;
        text("Press H to toggle the home/work location", 15, yLoc);
        yLoc += 20;
        text("Press N to proceed to the next sequence", 15, yLoc);
        yLoc += 20;
        text("Press P to return to the last sequence", 15, yLoc);
        yLoc += 20;
        text("Press A to show the aggregated Markov chain", 15, yLoc);
        yLoc += 20;
        text("Press Z to toggle the normalization of the chain", 15, yLoc);
        yLoc += 20;
        text("Press L to toggle the label of transitions", 15, yLoc);

        if (showVoronoi) {
            showVoronoi();
        }

        if (saveFile) {
            Location center = map.getLocationFromScreenPosition(width / 2,
                    height / 2);
            save(targetPath + File.separator + files.get(fileIndex).getName()
                    + "_level" + map.getZoomLevel() + "_" + +center.x + "_"
                    + center.y + ".png");
            saveFile = false;
        }

        if (showSeq) {
            pushStyle();
            textSize(30);
            fill(0xFFf52a76);
            textAlign(RIGHT);
            text("[" + files.get(fileIndex).getName() + "]", width, 30);
            popStyle();
            showSeq();
        }
        
        if (showHome) {
            showHomeAndWork();
        }

    }

    public void keyPressed() {
        switch (key) {
        case 'v':
            showVoronoi = !showVoronoi;
            break;

        case 's':
            saveFile = true;
            break;

        case 'q':
            showSeq = !showSeq;
            break;

        case 'h':
            showHome = !showHome;
            break;

        case 'n':
            fileIndex++;
            loadSeq();
            showAgg = false;
            break;

        case 'p':
            fileIndex = Math.max(fileIndex - 1, 0);
            loadSeq();
            showAgg = false;
            break;

        case 'a':
            loadEntireBCN();
            showSeq = !showSeq;
            showAgg = true;
            break;

        case 'z':
            normalize = !normalize;
            if (showAgg) {
                loadEntireBCN();
            } else {
                loadSeq();
            }
            break;

        case 'l':
            showLabel = !showLabel;
        }

    }
    public void loadEntireBCN() {
        tran2wt.clear();
        color2seq.clear();

        max_weight = -1;
        BufferedReader br;
        try {
            if (normalize) {
                br = new BufferedReader(new FileReader(Constants.RESULT_PATH
                        + File.separator + "10_one_big_markov_chain_of_BTS"
                        + File.separator + "3_normalized_big_chain"));
            } else {
                br = new BufferedReader(new FileReader(Constants.RESULT_PATH
                        + File.separator + "10_one_big_markov_chain_of_BTS"
                        + File.separator + "2_pruned_big_chain"));
            }

            String line = "";
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\\t");
                if (tokens[0].equals(tokens[1])) {
                    continue;
                }
                if (max_weight < Double.valueOf(tokens[2])) {
                    max_weight = Double.valueOf(tokens[2]);
                }
                tran2wt.put(new Transition(tokens[0], tokens[1]),
                        Double.valueOf(tokens[2]));
                List<String> locations = new ArrayList<String>();
                locations.add(tokens[0]);
                locations.add(tokens[1]);
                int edgeColor = color(random.nextInt(255), random.nextInt(255),
                        random.nextInt(255));
                color2seq.put(edgeColor, locations);
            }

            br.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        logger.debug("max_weight: " + max_weight);
        logger.debug("color2seq.size(): " + color2seq.size());
    }

    public void loadSeq() {
        File file = files.get(fileIndex);
        tran2wt.clear();

        max_weight = -1;
        BufferedReader br;
        try {
            if (normalize) {
                br = new BufferedReader(new FileReader(Constants.RESULT_PATH
                        + File.separator + "9_3_normalized_markov_chain_of_BTS"
                        + File.separator + file.getName()));
            } else {
                br = new BufferedReader(new FileReader(Constants.RESULT_PATH
                        + File.separator + "9_2_pruned_markov_chain_of_BTS"
                        + File.separator + file.getName()));
            }

            String line = "";
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\\t");
                if (BTS2Location.get(tokens[0]).equals(
                        BTS2Location.get(tokens[1]))) {
                    continue;
                }
                if (max_weight < Double.valueOf(tokens[2])) {
                    max_weight = Double.valueOf(tokens[2]);
                    logger.debug(tokens[0] + "->" + tokens[1] + ": "
                            + tokens[2]);
                }
                tran2wt.put(new Transition(tokens[0], tokens[1]),
                        Double.valueOf(tokens[2]));
            }
            br.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        color2seq.clear();

        try {
            br = new BufferedReader(new FileReader(files.get(fileIndex)));
            String line;

            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\\t");
                List<String> locations = new ArrayList<String>();

                // map one edgeColor into one sequence
                int edgeColor = color(random.nextInt(255), random.nextInt(255),
                        random.nextInt(255));

                // we should skip first two tokens (date & the length of
                // sequences)
                for (int i = 2; i < tokens.length; i++) {
                    locations.add(tokens[i]);
                }

                color2seq.put(edgeColor, locations);
            }
            br.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            logger.error(e);
        }
        logger.debug("max_weight: " + max_weight);

    }

    public void setup() {
        size(1900, 1000, GLConstants.GLGRAPHICS);
        smooth();
        gfx = new ToxiclibsSupport(this);
        font = createFont("Arial", 30);
        textFont(font);

        files = CDRUtil.loadFiles(Constants.RESULT_PATH + File.separator
                + "8_sequences_of_BTS_threshold_" + FindSequences.THRESHOLD_MIN
                + "_min", "^.*-.*$");
        Collections.sort(files, new RankComparator());

        // this is for the screenshots, but can't work due to the problem of libraries.
        targetPath = Constants.RESULT_PATH + File.separator
                + "13_visiualize_BTS_sequence";
        boolean success = (new File(targetPath)).mkdir();
        if (success) {
            logger.debug("A directory [" + targetPath + "] is created");
        }

        map = new Map(this, new Google.GoogleMapProvider());
        // map = new Map(this, new Microsoft.RoadProvider());
        // map = new Map(this, new Yahoo.HybridProvider());

        map.zoomAndPanTo(new Location(41.387628f, 2.1698f), 13); // lat-long
        MapUtils.createDefaultEventDispatcher(this, map);

        String line = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                    Constants.BARCELONA_CELL_INFO_PATH));
            while ((line = br.readLine()) != null) {
                if (line.startsWith("cell"))
                    continue; // skip the first line of column description
                Cell cell = new Cell(line);
                String btsID = cell.getBTSID();
                BTS2Location.put(btsID,
                        new Location((float) cell.getLatitude(), (float) cell.getLongitude()));
            }
        } catch (IOException e) {
            logger.error(e);
        } catch (ParseException e) {
            logger.error(e);
        }
        loadSeq();
        
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                    Constants.RESULT_PATH + File.separator + "17_1_home_BTS" + File.separator + "telnumber_homeBTS_threshold_1000m"));
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\t");
                String moviNum = tokens[0];
                String bts = tokens[1];
                num2home.put(moviNum, bts);
            }
            br.close();
            
            br = new BufferedReader(new FileReader(
                    Constants.RESULT_PATH + File.separator + "17_2_work_BTS" + File.separator + "telnumber_workBTS_threshold_1000m"));
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\t");
                String moviNum = tokens[0];
                String bts = tokens[1];
                num2work.put(moviNum, bts);
            }
            br.close();
        } catch (IOException ioe) {
            logger.error(ioe);
        }

    }

    public void showSeq() {
        for (int edgeColor : color2seq.keySet()) {
            List<String> locations = color2seq.get(edgeColor);
            for (int i = 1; i < locations.size(); i++) {
                Location ll = BTS2Location.get(locations.get(i - 1));
                Location cl = BTS2Location.get(locations.get(i));

                float lxy[] = this.map.getScreenPositionFromLocation(ll);
                float cxy[] = this.map.getScreenPositionFromLocation(cl);

                Transition t = new Transition(locations.get(i - 1),
                        locations.get(i));

                double weight = 0;
                if (tran2wt.get(t) == null) {
                    weight = 0;
                } else {
                    weight = tran2wt.get(t);
                }

                pushStyle();
                noStroke();
                fill(0xFF1f1adb, 40);

                ellipse(cxy[0], cxy[1], 20, 20);
                ellipse(lxy[0], lxy[1], 20, 20);

                int thickness = (int) map((float) weight, 0f,
                        (float) max_weight, 5f, 20f);
                int alpha = (int) map((float) weight, 0f, (float) max_weight,
                        50f, 255f);

                stroke(edgeColor, alpha);
                strokeWeight(thickness);

                line(lxy[0], lxy[1], cxy[0], cxy[1]);

                if (showLabel && !ll.equals(cl)) {
                    if (weight != 0) {
                        pushMatrix();
                        textSize(20);
                        fill(100, alpha);

                        String msg = "";
                        int yPos_adjustment = 0;
                        if (lxy[0] < cxy[0]) {
                            msg = "-> ";
                            yPos_adjustment = 10;
                        } else {
                            msg = "<- ";
                            yPos_adjustment = -10;
                        }

                        if (normalize) {
                            text(msg + String.format("%.2f", weight),
                                    (lxy[0] + cxy[0]) / 2, (lxy[1] + cxy[1])
                                            / 2 + yPos_adjustment);
                        } else {
                            text(msg + String.format("%.0f", weight),
                                    (lxy[0] + cxy[0]) / 2, (lxy[1] + cxy[1])
                                            / 2 + yPos_adjustment);
                        }
                        popMatrix();
                    }
                }

                popStyle();
            }
        }
    }
    
    public void showHomeAndWork() {
        pushStyle();
        textAlign(CENTER);
        textSize(25);
        fill(0xFFfb4b1d);
        String moviNum = files.get(fileIndex).getName().split("-")[1];
        String home = num2home.get(moviNum);
        String work = num2work.get(moviNum);
        
        if (home != null) {
            Location h = BTS2Location.get(home);
            float[] hxy = this.map.getScreenPositionFromLocation(h);
            text("H", hxy[0], hxy[1]);
        }
        
        if (work != null) {
            Location w = BTS2Location.get(work);
            float[] wxy = this.map.getScreenPositionFromLocation(w);
            text("W", wxy[0], wxy[1]);
        }
        
        popStyle();
    }

    public void showVoronoi() {
        voronoi = new Voronoi();
        for (String btsID : BTS2Location.keySet()) {
            Location location = BTS2Location.get(btsID);
            float xy[] = this.map.getScreenPositionFromLocation(location);
            if (xy[0] > width * 2 || xy[0] < -(width * 2) || xy[1] > height * 2
                    || xy[1] < -(height * 2)) {
                // avoid errors in toxiclib
                continue;
            }
            try {
                voronoi.addPoint(new Vec2D(xy[0], xy[1]));
            } catch (Exception e) {
                logger.debug(e);
            }
        }

        noFill();
        stroke(0xFF40a6dd);
        strokeWeight(1);

        for (Polygon2D polygon : voronoi.getRegions()) {
            gfx.polygon2D(polygon);
        }

        fill(255, 0, 255);
        noStroke();
        for (Vec2D c : voronoi.getSites()) {
            ellipse(c.x, c.y, 5, 5);
        }
    }
}
