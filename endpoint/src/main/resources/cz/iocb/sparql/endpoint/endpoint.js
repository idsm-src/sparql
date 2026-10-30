/*
 * The SPARQL editor of the endpoint: the web component of Ontotext (ontotext-yasgui-web-component), which the page
 * loads from a CDN, set up as the SPARQL GUI of IDSM, and fed with the prefixes, properties and classes of the
 * endpoint.
 */


const endpoint = window.location.protocol + "//" + window.location.host + window.location.pathname;

const info = fetch("?info", {
    method: "GET",
    headers: {
      "Accept": "application/json",
    },
    redirect: "follow",
  }).then(response => response.json());


const hiddenPlugins = ["pivot-table-plugin", "charts", "geo", "explain-plan"];


// the icons of the run button of the editor used before: the play icon, whose second outline runs around it during
// a query, and the warning about an invalid query
const queryIcon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72.900002 81.900002" aria-hidden="true">'
  + '<path d="m69.6 35.2-60.3-34.3c-2.2-1.2-4.4-1.2-6.4 0s-2.9 3.4-2.9 5.6v68.8c0 2.2 1.2 4.4 2.9 5.6 1 0.5'
  + ' 2.2 1 3.4 1s2.2-0.5 2.9-1l60.3-34.3c2.2-1.2 3.4-3.4 3.4-5.6s-1.1-4.3-3.3-5.8z"/>'
  + '<path class="loadingIcon" fill="none"'
  + ' d="m61.184 36.167-48.73-27.719c-1.7779-0.96976-3.5558-0.96976-5.172 0-1.6163 0.96976-2.3436'
  + ' 2.7476-2.3436 4.5255v55.599c0 1.7779 0.96976 3.5558 2.3436 4.5255 0.80813 0.40407 1.7779'
  + ' 0.80813 2.7476 0.80813 0.96975 0 1.7779-0.40406 2.3436-0.80813l48.73-27.719c1.7779-0.96976'
  + ' 2.7476-2.7476 2.7476-4.5255s-0.88894-3.475-2.6668-4.6872z"/>'
  + '</svg>';

const warningIcon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 66.399998 66.399998" aria-hidden="true"><g>'
  + '<path d="M33.2 0C14.9 0 0 14.9 0 33.2c0 18.3 14.9 33.2 33.2 33.2 18.3 0 33.2-14.9 33.2-33.2C66.4 14.9'
  + ' 51.5 0 33.2 0zm0 59.4C18.7 59.4 7 47.6 7 33.2 7 18.7 18.8 7 33.2 7c14.4 0 26.2 11.8 26.2 26.2'
  + ' 0 14.4-11.8 26.2-26.2 26.2z"/>'
  + '<path d="M33.1 45.6c-1.4 0-2.5.5-3.5 1.5-.9 1-1.4 2.2-1.4 3.6 0 1.6.5 2.8 1.5 3.8 1 .9 2.1 1.3 3.4 1.3'
  + ' 1.3 0 2.4-.5 3.4-1.4 1-.9 1.5-2.2 1.5-3.7 0-1.4-.5-2.6-1.4-3.6-.9-1-2.1-1.5-3.5-1.5zM33.3'
  + ' 12.4c-1.5 0-2.8.5-3.7 1.6-.9 1-1.4 2.4-1.4 4.2 0 1.1.1 2.9.2 5.6l.8 13.1c.2 1.8.4 3.2.9 4.1.5'
  + ' 1.2 1.5 1.8 2.9 1.8 1.3 0 2.3-.7 2.9-1.9.5-1 .7-2.3.9-4l1.1-13.4c.1-1.3.2-2.5.2-3.8'
  + ' 0-2.2-.3-3.9-.8-5.1-.5-1-1.6-2.2-4-2.2z"/>'
  + '</g></svg>';


// the download button offers the formats into which the shown results are saved, so that the query is not run again
const downloadFormats = [
  { labelKey: "yasr.plugin_control.download_as.sparql_results_json.label", value: "application/sparql-results+json" },
  { labelKey: "yasr.plugin_control.download_as.csv.label", value: "text/csv" },
];

const downloadAsConfig = {
  items: {
    SELECT: downloadFormats,
    CONSTRUCT: downloadFormats,
    DESCRIBE: downloadFormats,
  },
};


const PN_CHARS_BASE = `([A-Za-z\\u{00C0}-\\u{00D6}\\u{00D8}-\\u{00F6}\\u{00F8}-\\u{02FF}\\u{0370}-\\u{037D}\\u{037F}-\\u{1FFF}\\u{200C}-\\u{200D}\\u{2070}-\\u{218F}\\u{2C00}-\\u{2FEF}\\u{3001}-\\u{D7FF}\\u{F900}-\\u{FDCF}\\u{FDF0}-\\u{FFFD}\\u{10000}-\\u{EFFFF}])`;
const PN_CHARS_U = `(${PN_CHARS_BASE}|_)`;
const PN_CHARS = `(${PN_CHARS_U}|[-0-9\\u{00B7}\\u{0300}-\\u{036F}\\u{203F}-\\u{2040}])`;
const PERCENT = `(%[0-9A-Fa-f][0-9A-Fa-f])`;
const PN_LOCAL_ESC = `(\\\\[-_~.!$&'()*+,;=/?#@%])`;
const PLX = `(${PERCENT}|${PN_LOCAL_ESC})`;
const PN_LOCAL = `((${PN_CHARS_U}|[0-9:]|${PLX})((${PN_CHARS}|[.:]|${PLX})*(${PN_CHARS}|:|${PLX}))?)?`;
const localNameRegExp = new RegExp(`^${PN_LOCAL}$`, "u");


const htmlEscapes = { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" };


function escape(text) {
  return text.replace(/[&<>"']/g, c => htmlEscapes[c]);
}



/*
 * Abbreviates the IRI by the prefix giving the shortest local name, the shorter prefix on a tie.
 */
function getVisibleIri(iri, prefixes) {
  let visible = null;
  let size = iri.length;

  for(const prefix in prefixes ?? {}) {
    const definition = prefixes[prefix];

    if(!definition || !iri.startsWith(definition))
      continue;

    const name = iri.substring(definition.length);

    if((size > name.length || (size === name.length && visible !== null && visible.length - size - 1 > prefix.length))
        && name.match(localNameRegExp)) {
      visible = prefix + ":" + name;
      size = name.length;
    }
  }

  return visible;
}


function getIriLink(iri, href, prefixes) {
  const visible = getVisibleIri(iri, prefixes);
  const link = `<a class="iri" target="_blank" rel="noopener noreferrer" href="${escape(href)}">${escape(visible ?? iri)}</a>`;

  return visible !== null ? link : `&lt;${link}&gt;`;
}



function getLiteralContent(binding, prefixes) {
  const value = `"<span class="literal-value">${escape(binding.value)}</span>"`;

  if(binding["xml:lang"]) {
    const direction = binding["its:dir"] ? "--" + binding["its:dir"] : "";
    return `${value}<sup>@${escape(binding["xml:lang"] + direction)}</sup>`;
  }

  if(binding.datatype)
    return `${value}<sup>^^${getIriLink(binding.datatype, binding.datatype, prefixes)}</sup>`;

  return value;
}


function getTermContent(binding, prefixes) {
  switch(binding.type) {
    case "uri":
      return getIriLink(binding.value, binding.value, prefixes);

    case "literal":
    case "typed-literal":
      return getLiteralContent(binding, prefixes);

    case "bnode":
      return `_:${escape(binding.value)}`;

    case "triple":
      return "&lt;&lt;( "
        + getTermContent(binding.value.subject, prefixes) + " "
        + getTermContent(binding.value.predicate, prefixes) + " "
        + getTermContent(binding.value.object, prefixes) + " )&gt;&gt;";

    default:
      return escape(String(binding.value));
  }
}



/*
 * The cell renderer of the table; the term is a single element, so that the compact view of the table can shorten it.
 */
function getCellContent(binding, prefixes) {
  return `<div class="sparql-cell"><span class="sparql-term">${getTermContent(binding, prefixes)}</span></div>`;
}


/*
 * The web component loads the Yasgui library itself, so its globals can be adjusted only once the component is ready.
 */
function configureYasgui() {
  const Yasgui = window.Yasgui;

  Yasgui.defaults.endpointCatalogueOptions.getData = () => [{ endpoint: endpoint }];

  // the tabs are not kept across the visits of the page
  Yasgui.defaults.persistenceId = null;

  // a tab focuses its editor while the panel of the previous tab is still shown, so the page would scroll to the editor
  // below the results of that tab
  Yasgui.defaults.autofocus = false;

  // the pivot table, the charts and the map do not work well with the results, the charts load Google Charts, and the
  // explain plan needs GraphDB; the component registers them at each initialisation, so they are registered disabled
  const Yasr = Yasgui.Yasr;
  const registerPlugin = Yasr.registerPlugin;
  Yasr.registerPlugin = (name, plugin, enable) =>
    registerPlugin.call(Yasr, name, plugin, enable && !hiddenPlugins.includes(name));

  // the component applies its translations only after it has created the first tab, so the default name of a tab
  // cannot be translated
  const createTabName = Yasgui.prototype.createTabName;
  Yasgui.prototype.createTabName = function(name, i) {
    return createTabName.call(this, name || "Query", i);
  };

  const Yasqe = Yasgui.Yasqe;

  Yasqe.defaults.autocompleters = Yasqe.defaults.autocompleters.filter(name => name !== "property" && name !== "class");

  // the suggestions take the form of those of GraphDB, as only an IRI of that form is inserted in angle brackets or
  // with the prefix typed; the hints show what they insert, as HTML
  const getSuggestions = (iris, token) => iris.filter(iri => iri.startsWith(token.autocompletionString))
    .map(iri => ({ type: "uri", value: iri, description: iri }));
  const showInsertedText = (yasqe, hints) => hints.map(hint => ({ ...hint, displayText: escape(hint.text) }));

  Yasqe.forkAutocompleter("property", {
    name: "property-local",
    persistenceId: null,
    get: (yasqe, token) => info.then(data => getSuggestions(data.properties, token)).catch(() => []),
    postprocessHints: showInsertedText
  });

  Yasqe.forkAutocompleter("class", {
    name: "class-local",
    persistenceId: null,
    get: (yasqe, token) => info.then(data => getSuggestions(data.classes, token)).catch(() => []),
    postprocessHints: showInsertedText
  });

  // the run button shows the icons of the editor used before (see endpoint.css); it sets its label at each update, so
  // the icons replace the label afterwards; its tooltip, which offers the hidden menu of the button, is dropped, as it
  // is shown only when it has a text, and an empty translation falls back to the default one
  const updateQueryButton = Yasqe.prototype.updateQueryButton;
  Yasqe.prototype.updateQueryButton = function(...args) {
    updateQueryButton.apply(this, args);

    const button = this.queryBtn;

    if(!button)
      return;

    button.innerHTML = `<div class="svgImg queryIcon">${queryIcon}</div>`
      + `<div class="svgImg warningIcon">${warningIcon}</div>`;
    button.title = "Run query";
    button.setAttribute("aria-label", "Run query");
    button.parentElement?.setAttribute("yasgui-data-tooltip", "");
    button.parentElement?.removeAttribute("aria-label");
  };
}


/*
 * The download button only announces the chosen format and leaves the download to the client of the component, so the
 * results shown in the current tab are saved in that format.
 */
async function downloadResults(element, format) {
  const [data, extension] = format === "text/csv"
    ? [await element.getEmbeddedResultAsCSV(), "csv"]
    : [JSON.stringify(await element.getEmbeddedResultAsJson(), null, 2), "json"];

  if(!data)
    return;

  const link = document.createElement("a");
  link.href = URL.createObjectURL(new Blob([data], { type: format }));
  link.download = `queryResults.${extension}`;
  link.click();
  setTimeout(() => URL.revokeObjectURL(link.href));
}


/*
 * The component only announces the request to share a query and shows the link its client supplies. The link has the
 * form that Yasgui reads back on load (populateFromUrl).
 */
async function shareQuery(element, queryModel) {
  // the model carries the name and the query of the current tab, so openTab finds that tab and returns it
  const tab = await element.openTab(queryModel);
  const link = window.Yasgui.linkUtils.createShareLink(endpoint, tab);

  element.savedQueryConfig = { shareQueryLink: link };
}


window.addEventListener("load", async () => {
  const element = document.querySelector("ontotext-yasgui");

  await customElements.whenDefined("ontotext-yasgui");
  await element.componentOnReady();

  configureYasgui();

  const prefixes = await info.then(data => data.prefixes).catch(() => ({}));

  element.config = {
    endpoint: endpoint,
    query: "SELECT * WHERE\n{\n  ?S ?P ?O.\n}\nLIMIT 10",
    showControlBar: true,
    copyEndpointOnNewTab: false,
    populateFromUrl: true,
    method: "POST",
    // the default headers ask for JSON results whatever the query form and add X-GraphDB-Local-Consistency,
    // which the CORS preflight of other endpoints rejects
    headers: () => ({}),
    prefixes: prefixes,
    getCellContent: getCellContent,
    // the pagination expects GraphDB to page the results and to count them by a second run of the query
    paginationOn: false,
    yasqeActionButtons: [
      { name: "createSavedQuery", visible: false },
      { name: "showSavedQueries", visible: false },
      { name: "includeInferredStatements", visible: false },
    ],
    // the saved queries need GraphDB
    keyboardShortcutConfiguration: {
      create_save_query: false,
    },
    pluginsConfigurations: {
      extended_table: { downloadAsConfig },
      extended_response: { downloadAsConfig },
    },
  };

  element.addEventListener("shareQuery", event => shareQuery(element, event.detail));
  element.addEventListener("output", event => {
    if(event.detail?.TYPE === "downloadAs")
      downloadResults(element, event.detail.payload.value);
  });

  // a query of a shared link is read once the editor is set up, and the address shows the endpoint alone afterwards
  await element.getQuery();
  window.history.replaceState({}, document.title, endpoint);
});
