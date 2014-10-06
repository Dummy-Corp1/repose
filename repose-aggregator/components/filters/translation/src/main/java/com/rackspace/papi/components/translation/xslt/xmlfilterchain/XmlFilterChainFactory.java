package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.components.translation.config.StyleSheet;
import com.rackspace.papi.components.translation.config.TranslationBase;
import com.rackspace.papi.components.translation.xslt.StyleSheetInfo;
import org.apache.commons.pool.PoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

public class XmlFilterChainFactory implements PoolableObjectFactory<XmlFilterChain> {

    private static final Logger LOG = LoggerFactory.getLogger(XmlFilterChainFactory.class);
    private final XmlFilterChainBuilder builder;
    private final TranslationBase translation;
    private final String configRoot;
    private final String config;

    public XmlFilterChainFactory(final XmlFilterChainBuilder xsltChainBuilder, final TranslationBase translation, final String configRoot, final String config) {
        this.builder = xsltChainBuilder;
        this.translation = translation;
        this.configRoot = configRoot;
        this.config = config;
    }

    private String getAbsoluteXslPath(String xslPath) {
        return !xslPath.contains("://") ? StringUtilities.join("file://", configRoot, "/", xslPath) : xslPath;
    }

    @Override
    public XmlFilterChain makeObject() {
        List<StyleSheetInfo> stylesheets = new ArrayList<StyleSheetInfo>();
        if (translation.getStyleSheets() != null) {
            for (StyleSheet sheet : translation.getStyleSheets().getStyle()) {
                if (sheet.getXsl() != null && sheet.getXsl().getAny() != null) {
                    stylesheets.add(new StyleSheetInfo(sheet.getId(), (Node)sheet.getXsl().getAny(), getAbsoluteXslPath(config)));
                } else {
                    stylesheets.add(new StyleSheetInfo(sheet.getId(), getAbsoluteXslPath(sheet.getHref())));
                }
            }
        }

        return builder.build(stylesheets.toArray(new StyleSheetInfo[0]));
    }

    @Override
    public void destroyObject(XmlFilterChain xmlFilterChain) throws Exception {
        LOG.debug("No action required to destroy: {}", xmlFilterChain);
    }

    @Override
    public boolean validateObject(XmlFilterChain xmlFilterChain) {
        return xmlFilterChain != null;
    }

    @Override
    public void activateObject(XmlFilterChain xmlFilterChain) throws Exception {
        LOG.debug("No action required to activate: {}", xmlFilterChain);
    }

    @Override
    public void passivateObject(XmlFilterChain xmlFilterChain) throws Exception {
        LOG.debug("No action required to passivate: {}", xmlFilterChain);
    }
}
