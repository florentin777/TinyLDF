const App = {
  subject: "",
  predicate: "",
  object: "",
  results: null, // pour stocker les résultats de la requête
  loading: false,
  error: null,

  fetchResults: function() {
    App.loading = true;
    App.results = null;
    App.error = null;

    let params = [];
    if (App.subject) params.push('subject=' + encodeURIComponent(App.subject));
    if (App.predicate) params.push('predicate=' + encodeURIComponent(App.predicate));
    if (App.object) params.push('object=' + encodeURIComponent(App.object));

    let url = '/_ah/api/myTinyLDF/v1/ldf?' + params.join('&');
 
    // m.request permet de faire une requête AJAX. On suppose que ton endpoint renvoie du texte (N-Quads).
    // Si ton endpoint renvoie du JSON, remplace 'text' par 'json' et adapte la logique.
    m.request({
      method: "GET",
      url: url,
      deserialize: x => x // on suppose un format texte brut
    }).then(data => {
      App.loading = false;
      App.results = data;
    }).catch(err => {
      App.loading = false;
      App.error = err.message;
    });
  },

  view: () =>
    m("div", { style: "max-width: 800px; margin: 0 auto; font-family: Arial, sans-serif;" }, [
      // Header
      m("header", [
        m("h1", { style: "display: inline-block;" },
          m("a", { href: "/", style: "text-decoration: none; color: black;" }, "Wikidata")
        ),
        m("figure", { class: "logo", style: "float: right;" }, [
          m("a", m("img", { src: "assets/logo.svg", alt: "Linked Data Fragments", style: "width: 160px;" }))
        ])
      ]),

      // Main Content
      m("main", [
        m("h2", "Wikidata"),

        // Formulaire sans action/method, géré par Mithril
        m("form", {
          onsubmit: function(e) {
            e.preventDefault(); // Empêche le rechargement de la page
            App.fetchResults();
          },
          property: "hydra:search", resource: "#triplePattern", style: "margin-bottom:20px;"
        }, [
          m("fieldset", { resource: "#triplePattern", style: "border: none;" }, [
            m("legend", "Query Wikidata by triple pattern"),
            m("ul", { style: "list-style: none; padding: 0;" }, [
              m("li", { style: "margin-bottom: 10px;" }, [
                m("label", { for: "subject", style: "display: block; font-weight: bold;" }, "Subject"),
                m("input", {
                  class: "uri",
                  id: "subject",
                  name: "subject",
                  style: "width: 100%; padding: 5px;",
                  oninput: (e) => App.subject = e.target.value,
                  value: App.subject
                })
              ]),
              m("li", { style: "margin-bottom: 10px;" }, [
                m("label", { for: "predicate", style: "display: block; font-weight: bold;" }, "Predicate"),
                m("input", {
                  class: "uri",
                  id: "predicate",
                  name: "predicate",
                  style: "width: 100%; padding: 5px;",
                  oninput: (e) => App.predicate = e.target.value,
                  value: App.predicate
                })
              ]),
              m("li", { style: "margin-bottom: 10px;" }, [
                m("label", { for: "object", style: "display: block; font-weight: bold;" }, "Object"),
                m("input", {
                  class: "uri",
                  id: "object",
                  name: "object",
                  style: "width: 100%; padding: 5px;",
                  oninput: (e) => App.object = e.target.value,
                  value: App.object
                })
              ])
            ])
          ]),
          m("p", [
            m("input", {
              type: "submit",
              value: "Find matching triples",
              style: "font-weight: bold; color: white; background-color: #be1622; padding: 5px 10px; border: none; cursor: pointer;"
            })
          ])
        ]),

        // Affichage des résultats
        App.loading ? m("p", "Loading...") : null,
        App.error ? m("p", {style:"color:red;"}, "Error: " + App.error) : null,
        App.results ? 
          m("div", [
            m("h3", "Matches in Wikidata"),
            m("pre", {style:"white-space: pre-wrap; word-wrap: break-word;"}, App.results)
          ])
        : null
      ]),

      // Footer
      m("footer", { style: "margin-top: 20px; text-align: center;" }, [
        m("p", [
          "Powered by a ",
          m("a", { href: "https://github.com/florentin777/TinyLDF", target: "_blank", style: "color: gray;" }, "Linked Data Fragments Server"),
          " ©2013–2024 Ghent University - imec"
        ])
      ])
    ])
};

// Monte l'application Mithril dans le body
m.mount(document.body, App);

