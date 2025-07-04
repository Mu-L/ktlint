package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CATCH
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FINALLY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TRY
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment20
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.parent
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet

/**
 * Checks spacing and wrapping of try-catch-finally.
 */
@SinceKtlint("0.49", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class TryCatchFinallySpacingRule :
    StandardRule(
        id = "try-catch-finally-spacing",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ),
    Rule.OfficialCodeStyle {
    private var indentConfig = DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isPartOfComment20 && node.parent?.elementType in TRY_CATCH_FINALLY_TOKEN_SET) {
            emit(node.startOffset, "No comment expected at this location", false)
            return
        }
        when (node.elementType) {
            BLOCK -> {
                visitBlock(node, emit)
            }

            CATCH, FINALLY -> {
                visitClause(node, emit)
            }
        }
    }

    private fun visitBlock(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.parent?.elementType !in TRY_CATCH_FINALLY_TOKEN_SET) {
            return
        }

        node
            .findChildByType(LBRACE)!!
            .let { lbrace ->
                val nextSibling = lbrace.nextSibling { !it.isPartOfComment20 }!!
                if (!nextSibling.text.startsWith("\n")) {
                    emit(lbrace.startOffset + 1, "Expected a newline after '{'", true)
                        .ifAutocorrectAllowed {
                            lbrace.upsertWhitespaceAfterMe(indentConfig.siblingIndentOf(node))
                        }
                }
            }

        node
            .findChildByType(RBRACE)!!
            .let { rbrace ->
                val prevSibling = rbrace.prevSibling { !it.isPartOfComment20 }!!
                if (!prevSibling.text.startsWith("\n")) {
                    emit(rbrace.startOffset, "Expected a newline before '}'", true)
                        .ifAutocorrectAllowed {
                            rbrace.upsertWhitespaceBeforeMe(indentConfig.parentIndentOf(node))
                        }
                }
            }
    }

    private fun visitClause(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        val prevLeaf = node.prevLeaf { !it.isPartOfComment20 }!!
        if (prevLeaf.text != " ") {
            emit(node.startOffset, "A single space is required before '${node.elementTypeName()}'", true)
                .ifAutocorrectAllowed {
                    node.upsertWhitespaceBeforeMe(" ")
                }
        }
    }

    private fun ASTNode.elementTypeName() = elementType.toString().lowercase()

    private companion object {
        val TRY_CATCH_FINALLY_TOKEN_SET = TokenSet.create(TRY, CATCH, FINALLY)
    }
}

public val TRY_CATCH_RULE_ID: RuleId = TryCatchFinallySpacingRule().ruleId
