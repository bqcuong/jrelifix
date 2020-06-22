package net.bqc.jrelifix.search.cs

import net.bqc.jrelifix.context.diff.ChangeSnippet

trait IChangeSnippetCondition {
    def satisfied(cs: ChangeSnippet): Boolean
}
