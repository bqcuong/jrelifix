package net.bqc.jrelifix.search

import net.bqc.jrelifix.context.diff.ChangeSnippet

trait IChangeSnippetCondition {
    def satisfied(cs: ChangeSnippet): Boolean
}
