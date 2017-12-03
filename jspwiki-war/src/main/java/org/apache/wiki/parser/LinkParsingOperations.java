/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */
package org.apache.wiki.parser;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.wiki.VariableManager;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.ProviderException;


/**
 * Link parsing operations
 */
public class LinkParsingOperations {

    private static Logger log = Logger.getLogger( LinkParsingOperations.class );
    private final WikiContext wikiContext;

    /**
     *  This list contains all IANA registered URI protocol
     *  types as of September 2004 + a few well-known extra types.
     *
     *  JSPWiki recognises all of them as external links.
     *
     *  This array is sorted during class load, so you can just dump
     *  here whatever you want in whatever order you want.
     */
    static final String[] EXTERNAL_LINKS = {
        "http:", "ftp:", "https:", "mailto:",
        "news:", "file:", "rtsp:", "mms:", "ldap:",
        "gopher:", "nntp:", "telnet:", "wais:",
        "prospero:", "z39.50s", "z39.50r", "vemmi:",
        "imap:", "nfs:", "acap:", "tip:", "pop:",
        "dav:", "opaquelocktoken:", "sip:", "sips:",
        "tel:", "fax:", "modem:", "soap.beep:", "soap.beeps",
        "xmlrpc.beep", "xmlrpc.beeps", "urn:", "go:",
        "h323:", "ipp:", "tftp:", "mupdate:", "pres:",
        "im:", "mtqp", "smb:"
    };

    static {
        Arrays.sort( EXTERNAL_LINKS );
    }

    public LinkParsingOperations( final WikiContext wikiContext ) {
    	this.wikiContext = wikiContext;
    }

    /**
     * @see {@link JSPWikiMarkupParser#isAccessRule()}
     */
    public boolean isAccessRule( final String link ) {
        return link.startsWith("{ALLOW") || link.startsWith("{DENY");
    }

    /**
     * @see {@link JSPWikiMarkupParser#isPluginLink()}
     */
    public boolean isPluginLink( final String link ) {
        return link.startsWith( "{INSERT" ) ||
               ( link.startsWith( "{" ) && !link.startsWith( "{$" ) );
    }

    /**
     * @see {@link JSPWikiMarkupParser#isMetadata()}
     */
    public boolean isMetadata( final String link ) {
        return link.startsWith( "{SET" );
    }

    /**
     * @see {@link VariableManager#isVariableLink()}
     */
    public boolean isVariableLink( String link ) {
        return link.startsWith( "{$" );
    }

    /**
     * something similar at {@link LinkParser.Link#isInterwikiLink()}.
     * @param page
     * @return
     */
    public boolean isInterWikiLink( final String page ) {
        return page.indexOf( ':' ) != -1;
    }

    /**
     *  Figures out if a link is an off-site link.  This recognizes
     *  the most common protocols by checking how it starts.
     *
     *  @param link The link to check.
     *  @return true, if this is a link outside of this wiki.
     *  @since 2.10.3
     */
    public boolean isExternalLink( final String page ) {
        int idx = Arrays.binarySearch( EXTERNAL_LINKS, page, new StartingComparator() );

        //
        // We need to check here once again; otherwise we might get a match for something like "h".
        //
        if( idx >= 0 && page.startsWith( EXTERNAL_LINKS[ idx ] ) ) {
            return true;
        }

        return false;
    }

    /**
     * @see at {@link JSPWikiMarkupParser#isImageLink(String)}.
     */
    public boolean isImageLink( String link ) {
        if( wikiContext.getEngine().getRenderingManager().getParser( wikiContext, link ).isImageInlining() )
        {
            link = link.toLowerCase();
            List< Pattern > inlineImagePatterns = wikiContext.getEngine().getRenderingManager()
            		                                         .getParser( wikiContext, link ).getInlineImagePatterns();

            for( Iterator< Pattern >  i = inlineImagePatterns.iterator(); i.hasNext(); ) {
                if( new Perl5Matcher().matches( link, i.next() ) )
                    return true;
            }
        }

        return false;
    }

    public boolean linkExists( final String page ) {
        if( page == null || page.length() == 0 ) {
            return false;
        }
        try {
            return wikiContext.getEngine().getFinalPageName( page ) != null;
        } catch( ProviderException e ) {
            log.warn( "TranslatorReader got a faulty page name [" + page + "]!", e );
            return false;
        }
    }

    public String linkIfExists( final String page ) {
        if( page == null || page.length() == 0 ) {
            return null;
        }
        try {
            return wikiContext.getEngine().getFinalPageName( page );
        } catch( ProviderException e ) {
            log.warn( "TranslatorReader got a faulty page name [" + page + "]!", e );
            return null;
        }
    }

    /**
     * Compares two Strings, and if one starts with the other, then returns null. Otherwise just like the normal Comparator for strings.
     *
     * @since
     */
    private static class StartingComparator implements Comparator< String > {

        /**
         * {@inheritDoc}
         *
         * @see Comparator#compare(String, String)
         */
        @Override
        public int compare( final String s1, final String s2 ) {
            if( s1.length() > s2.length() ) {
                if( s1.startsWith( s2 ) && s2.length() > 1 ) {
                    return 0;
                }
            } else if( s2.startsWith( s1 ) && s1.length() > 1 ) {
                return 0;
            }

            return s1.compareTo( s2 );
        }

    }

}
