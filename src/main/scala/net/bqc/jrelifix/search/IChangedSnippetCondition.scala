package net.bqc.jrelifix.search

import net.bqc.jrelifix.context.diff.ChangedSnippet

trait IChangedSnippetCondition {
    def satisfied(changedSnippet: ChangedSnippet): Boolean
}
