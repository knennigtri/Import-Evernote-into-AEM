	/*
 * ADOBE CONFIDENTIAL
 *
 * Copyright 2014 Adobe Systems Incorporated
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 */
;
(function ($, ns, channel, window, undefined) {

    var self = {},
        name = 'Evernote Notes';

        // make the loadAssets fuction more flexible
    self.searchRoot = '/content/dam/evernote-sync';

    var searchPath = self.searchRoot,
        imageServlet = '/bin/wcm/contentfinder/asset/view.html',
        itemResourceType = 'cq/gui/components/authoring/assetfinder/asset';

    
    /**
     *
     * @param query {String} search query
     * @param lowerLimit {Number} lower bound for paging
     * @param upperLimit {Number} upper bound for paging
     * @returns {jQuery.Promise}
     */
    self.loadAssets = function (query, lowerLimit, upperLimit) {
        // the image servlet is now used
        // though a different rendering will be necessary for extensions
        var param = {
                '_dc': new Date().getTime(),  // cache killer
                'query': query,
                'mimeType': 'text/html,text,application/xhtml+xml',
                'itemResourceType': itemResourceType, // single item rendering (cards)
                'limit': lowerLimit + ".." + upperLimit,
                '_charset_': 'utf-8'
            };

        return $.ajax({
            type: 'GET',
            dataType: 'html',
            url: Granite.HTTP.externalize(imageServlet) + searchPath,
            data: param
        });
    };

    /**
     * Set URL to image servlet
     * @param servlet {String} URL to image servlet
     */
    self.setServlet = function (imgServlet) {
        imageServlet = imgServlet;
    };

    self.setSearchPath = function (spath) {
        searchPath = spath;
    };

    self.setItemResourceType = function (rt) {
        itemResourceType = rt;
    };

    self.resetSearchPath = function () {
        searchPath = self.searchRoot;
    };

    // register as a asset tab
    ns.ui.assetFinder.register(name, self);

}(jQuery, Granite.author, jQuery(document), this));
