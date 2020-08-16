package net.bqc.lyfix.search.cs

import net.bqc.lyfix.context.diff.ChangeSnippet

trait IChangeSnippetCondition {
    def satisfied(cs: ChangeSnippet): Boolean
}
