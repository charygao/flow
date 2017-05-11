/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.uitest.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.html.Button;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.html.Hr;
import com.vaadin.server.InputStreamFactory;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResourceRegistration;
import com.vaadin.ui.AttachEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasText;
import com.vaadin.ui.Text;
import com.vaadin.ui.UI;

public class DependencyView extends AbstractDivView {

    private StreamResourceRegistration htmlImport2;
    private StreamResourceRegistration htmlImport3;

    @Tag("div")
    @HtmlImport("/test-files/html/orderedHtmlImport.html")
    static class HtmlComponent extends Component implements HasText {

        public HtmlComponent() {
            setText("Text component");
        }
    }

    @Override
    protected void onShow() {
        add(new Text(
                "This test initially loads a stylesheet which makes all text red, a JavaScript for logging window messages, a JavaScript for handling body click events and an HTML which sends a window message"),
                new Hr(), new HtmlComponent(), new Hr());

        Div clickBody = new Div();
        clickBody.setText("Hello, click the body please");
        clickBody.setId("hello");
        add(clickBody);

        Button jsOrder = new Button("Test JS order", e -> {
            getPage().addJavaScript("/test-files/js/set-global-var.js");
            getPage().addJavaScript("/test-files/js/read-global-var.js", false);
        });
        jsOrder.setId("loadJs");

        /* HTML imports */
        Button htmlOrder = new Button("Test HTML order", e -> {
            getPage().addHtmlImport(htmlImport2.getResourceUri().toString());

            // This failure can only be seen in the browser console
            getPage().addHtmlImport("/doesnotexist.html");

            // Can't test JS/HTML order because of #765
            getPage().addHtmlImport(htmlImport3.getResourceUri().toString());
        });
        htmlOrder.setId("loadHtml");

        /* HTML & JS order */
        Button mixedOrder = new Button("Test HTML & JS order", e -> {
            getPage().addHtmlImport("/test-files/html/combinedMixed.html");
        });
        mixedOrder.setId("loadMixed");

        Button allBlue = new Button("Load 'everything blue' stylesheet", e -> {
            getPage().addStyleSheet("/test-files/css/allblueimportant.css");

        });
        allBlue.setId("loadBlue");

        Button loadUnavailableResources = new Button(
                "Load unavailable resources", e -> {
                    getPage().addStyleSheet("/not-found.css");
                    getPage().addHtmlImport("/not-found.html");
                    getPage().addJavaScript("/not-found.js");
                });
        loadUnavailableResources.setId("loadUnavailableResources");

        Div log = new Div();
        log.setId("log");

        add(jsOrder, htmlOrder, mixedOrder, allBlue, loadUnavailableResources,
                new Hr(), log);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        htmlImport2 = registerResource(ui, "htmlimport2.html",
                new HTMLImportStreamFactory("HTML import 2", 1000));
        htmlImport3 = registerResource(ui, "htmlimport3.html",
                new HTMLImportStreamFactory("HTML import 3", 0));

        getPage().addStyleSheet("/test-files/css/allred.css");
        getPage().addJavaScript("/test-files/js/body-click-listener.js");
        getPage().addHtmlImport("/test-files/html/htmlimport1.html");
    }

    public static StreamResourceRegistration registerResource(UI ui,
            String name, InputStreamFactory streamFactory) {
        return ui.getSession().getResourceRegistry()
                .registerResource(new StreamResource(name, streamFactory));
    }

    public static class JSStreamFactory implements InputStreamFactory {
        private String name;
        private int delay;

        public JSStreamFactory(String name, int delay) {
            this.name = name;
            this.delay = delay;
        }

        @Override
        public InputStream createInputStream() {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                // Ignore
            }
            return stringToStream("window.logMessage('" + name + " loaded');");
        }

        protected InputStream stringToStream(String jsString) {
            byte[] bytes = jsString.getBytes(StandardCharsets.UTF_8);
            return new ByteArrayInputStream(bytes);
        }
    }

    public static class HTMLImportStreamFactory extends JSStreamFactory {

        public HTMLImportStreamFactory(String name, int delay) {
            super(name, delay);
        }

        @Override
        protected InputStream stringToStream(String jsString) {
            return super.stringToStream(
                    "<script type='text/javascript'>" + jsString + "</script>");
        }
    }

}