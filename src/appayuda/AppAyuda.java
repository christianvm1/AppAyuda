/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appayuda;

import java.io.InputStream;
import java.net.URL;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebHistory.Entry;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import netscape.javascript.JSObject;

/**
 *
 * @author chris
 */
public class AppAyuda extends Application {
    
    private Scene scene;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Web View");
        scene = new Scene(new Browser(),750,500, Color.web("#666970"));
        primaryStage.setScene(scene);
        //scene.getStylesheets().add(App.class.getResource("BrowserToolbar.css").toExternalForm());
        primaryStage.show();

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}

class Browser extends Region {
    private HBox toolBar;
    private static String[] imageFiles = new String[]{
        "/images/product.png",
        "/images/blog.png",
        "/images/documentation.png",
        "/images/partners.png",
        "/images/help.png",
        "/images/help.png",
        "/images/moodle.jpg",
        "/images/facebook.jpg",
        "/images/twitter.jpg"
    };
    private static String[] captions = new String[]{
        "Products",
        "Blogs",
        "Documentation",
        "Partners",
        "Help",
        "MiHtml",
        "Moodle",
        "Facebook",
        "Twitter"
    };
    private static String[] urls = new String[]{
        "http://www.oracle.com/products/index.html",
        "http://blogs.oracle.com/",
        "http://docs.oracle.com/javase/index.html",
        "http://www.oracle.com/partners/index.html",
        AppAyuda.class.getResource("help.html").toExternalForm(),
        AppAyuda.class.getResource("/miHtml/RS_index.html").toExternalForm(),
        "http://aula.ieslosmontecillos.es/",
        "https://es-es.facebook.com/",
        "https://twitter.com/?lang=es"
    };
    final ImageView selectedImage = new ImageView();
    final Hyperlink[] hpls = new Hyperlink[captions.length];
    final Image[] images = new Image[imageFiles.length];
    
    final Button toggleHelpTopics = new Button("Toggle Help Topics");
    final WebView smallView = new WebView();
    private boolean needDocumentationButton = false;

    final WebView browser = new WebView();
    final WebEngine webEngine = browser.getEngine();
    
    // habrá que definir la combo como propiedad de la clase Brower
    final ComboBox comboBox = new ComboBox();
    
    public Browser() {
        //apply the styles
        getStyleClass().add("browser");
        
        //Para tratar lo tres enlaces
        for (int i = 0; i < captions.length; i++) {
            Hyperlink hpl = hpls[i] = new Hyperlink(captions[i]);
            Image image = images[i] = new Image(getClass().getResourceAsStream(imageFiles[i]));
            hpl.setGraphic(new ImageView (image));
            final String url = urls[i];
            final boolean addButton = (hpl.getText().equals("Help"));

            //proccess event
            hpl.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    needDocumentationButton = addButton;
                    webEngine.load(url);
                }
            });
        }
        
        // load the web page
        webEngine.load("http://www.oracle.com/products/index.html");
        // create the toolbar
        toolBar = new HBox();
        toolBar.setAlignment(Pos.CENTER);
        toolBar.getStyleClass().add("browser-toolbar");
        toolBar.getChildren().addAll(hpls);
        toolBar.getChildren().add(createSpacer());
        
        //En el constructor de la clase Browser damos formato al combobox y lo
        //incluimos en la toolbar
        comboBox.setPrefWidth(60);
        toolBar.getChildren().add(comboBox);
        
        //también el constructor de la clase Browser declaramos el manejador
        //del histórico
        
        //process history
        final WebHistory history = webEngine.getHistory();
        history.getEntries().addListener(
            (ListChangeListener.Change<? extends WebHistory.Entry> c) -> {
                c.next();
                c.getRemoved().stream().forEach((e) -> {
                comboBox.getItems().remove(e.getUrl());
            });
                c.getAddedSubList().stream().forEach((e) -> {
                comboBox.getItems().add(e.getUrl());
            });
        });
 
        //set the behavior for the history combobox   
        comboBox.setOnAction((Event ev) -> {
            int offset = comboBox.getSelectionModel().getSelectedIndex()- history.getCurrentIndex();
            history.go(offset);
        });
        
        //set action for the button
        toggleHelpTopics.setOnAction(new EventHandler() {
            @Override
            public void handle(Event t) {
                webEngine.executeScript("toggle_visibility('help_topics')");
            }
        });
        
        smallView.setPrefSize(120, 80);
        //handle popup windows
        webEngine.setCreatePopupHandler(
            new Callback<PopupFeatures, WebEngine>() {
                @Override public WebEngine call(PopupFeatures config) {
                    smallView.setFontScale(0.8);
                    if (!toolBar.getChildren().contains(smallView)) {
                        toolBar.getChildren().add(smallView);
                    }
                    return smallView.getEngine();
                }
            }
        );

        // process page loading
        webEngine.getLoadWorker().stateProperty().addListener(
            new ChangeListener<State>() {
                @Override
                public void changed(ObservableValue<? extends State> ov, State oldState, State newState) {
                    toolBar.getChildren().remove(toggleHelpTopics);
                    if (newState == State.SUCCEEDED) {
                        JSObject win = (JSObject) webEngine.executeScript("window");
                        win.setMember("app", new JavaApp());

                        if (needDocumentationButton) {
                            toolBar.getChildren().add(toggleHelpTopics);
                        }
                    }
                }
            }
        ); 
        // load the web page
        webEngine.load("http://www.oracle.com/products/index.html ");

        //add components
        getChildren().add(toolBar);
        getChildren().add(browser);
    }
    
    // JavaScript interface object
    public class JavaApp {
        public void exit() {
            Platform.exit();
        }
    }
 
    private Node createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        double tbHeight = toolBar.prefHeight(w);
        layoutInArea(browser,0,0,w,h-tbHeight,0, HPos.CENTER, VPos.CENTER);
        layoutInArea(toolBar,0,h-tbHeight,w,tbHeight,0,HPos.CENTER,VPos.CENTER);

    }
    @Override
    protected double computePrefWidth(double height) {
        return 750;
    }
    @Override
    protected double computePrefHeight(double width) {
        return 500;
    }
}
