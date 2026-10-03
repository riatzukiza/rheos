(ns rheos.backend.shape.frontmatter-serialization-test
  (:require #?(:clj [clojure.test :refer [deftest is]]
               :cljs [cljs.test :refer [deftest is]])
            [rheos.backend.shape.content-parser :as parser]))

(deftest quoted-scalar-encoding-is-portable
  (let [value (str "quote\" slash\\ LF\n CR\r tab\t "
                   (apply str (map char [0 1 8 12 31 133 8232 8233])))
        encoded "\"quote\\\" slash\\\\ LF\\n CR\\r tab\\t \\u0000\\u0001\\u0008\\u000c\\u001f\\u0085\\u2028\\u2029\""]
    (is (= (str "---\ntitle: " encoded "\n---")
           (parser/serialize-frontmatter [[:title value]])))
    (is (= (str "---\nlabels: [" encoded ", \"7\", \"false\", \"\"]\n---")
           (parser/serialize-frontmatter [[:labels [value 7 false nil]]])))))
