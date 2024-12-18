const App = {
  view: () =>
    m("div", { style: "max-width: 800px; margin: 0 auto; font-family: Arial, sans-serif;" }, [
      // Header
      m("header", [
        m("h1", { style: "display: inline-block;" }, m("a", { href: "/", style: "text-decoration: none; color: black;" }, "Wikidata")),
        m("figure", { class: "logo", style: "float: right;" }, [
          m("a", m("img", { src: "assets/logo.svg", alt: "Linked Data Fragments", style: "width: 160px;" }))
        ])
      ]),

      // Main Content
      m("main", [
        m("h2", "Wikidata"),

        // Form
        m("form", { action: "?", method: "GET", property: "hydra:search", resource: "#triplePattern" }, [
          m("fieldset", { resource: "#triplePattern", style: "border: none;" }, [
            m("legend", "Query Wikidata by triple pattern"),
            m("ul", { style: "list-style: none; padding: 0;" }, [
              ["subject", "Subject"],
              ["predicate", "Predicate"],
              ["object", "Object"]
            ].map(([id, label]) =>
              m("li", { property: "hydra:mapping", resource: `#${id}`, style: "margin-bottom: 10px;" }, [
                m("label", { for: id, style: "display: block; font-weight: bold;" }, label),
                m("input", {
                  class: "uri",
                  id,
                  name: id,
                  about: `#${id}`,
                  property: "hydra:property",
                  resource: `rdf:${id}`,
                  style: "width: 100%; padding: 5px;"
                })
              ])
            ))
          ]),
          m("p", [
            m("input", {
              type: "submit",
              value: "Find matching triples",
              style: "font-weight: bold; color: white; background-color: #be1622; padding: 5px 10px; border: none; cursor: pointer;"
            })
          ])
        ]),

        // Matches Section
        m("h3", "Matches in Wikidata for ", m("em", { class: "pattern" })),
        m("div", { class: "counts", style: "color: gray;" }, [
          "Showing triples {a} to {b} of ± ",
          m("span", { property: "void:triples hydra:totalItems", datatype: "xsd:integer" }, "{nb_ttl}"),
          " with ",
          m("span", { property: "hydra:itemsPerPage", datatype: "xsd:integer" }, "100"),
          " triples per page.",
          m("ul", { class: "links", style: "margin: 0; padding: 0;" }, [
            m("li", { style: "display: inline;" }, "next")
          ])
        ]),

        // Triples List
        m("ul", { class: "triples", style: "font-family: 'Droid Sans Mono', monospace; overflow-x: auto;" }, [
          m("li", [
            m("a", { href: "?subject=http%3A%2F%2Fwikiba.se%2Fontology%23Dump" }, "Dump"),
            m("a", { href: "?predicate=http%3A%2F%2Fcreativecommons.org%2Fns%23license", style: "margin: 0 1em;" }, "license"),
            m("a", { href: "?object=http%3A%2F%2Fcreativecommons.org%2Fpublicdomain%2Fzero%2F1.0%2F" }, "Public Domain")
          ])
        ]),

        m("ul", { class: "links", style: "margin-top: 20px;" }, [m("li", "next")])
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

// Nettoie le contenu du body avant le montage
m.mount(document.body, App);
