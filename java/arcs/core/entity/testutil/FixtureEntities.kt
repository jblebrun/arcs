package arcs.core.entity.testutil

import arcs.core.entity.Reference
import arcs.core.storage.testutil.DummyStorageKey
import arcs.core.util.ArcsInstant
import arcs.core.util.BigInt
import arcs.core.util.toBigInt

typealias FixtureEntity = AbstractTestParticle.FixtureEntity
typealias InnerEntity = AbstractTestParticle.InnerEntity
typealias MoreInline = AbstractTestParticle.MoreInline

/**
 * Generates entities with a large number of field types, to be used in tests.
 */
class FixtureEntities {
  private var entityCounter = 0
  private var innerEntityCounter = 0
  private var moreInlineCounter = 0

  /**
   * Every call to [generate] will return an entity with the same schema but different field values.
   */
  fun generate(): FixtureEntity {
    entityCounter++
    return FixtureEntity(
      textField = "text $entityCounter",
      numField = entityCounter.toDouble(),
      boolField = entityCounter % 2 == 0,
      byteField = entityCounter.toByte(),
      shortField = entityCounter.toShort(),
      intField = entityCounter,
      longField = entityCounter.toLong(),
      charField = 'a',
      floatField = entityCounter.toFloat(),
      doubleField = entityCounter.toDouble(),
      instantField = ArcsInstant.ofEpochMilli(entityCounter.toLong()),
      bigintField = entityCounter.toBigInt(),
      boolsField = setOf(true, false),
      numsField = setOf(-1.0, entityCounter.toDouble()),
      textsField = setOf("a", "$entityCounter"),
      bytesField = setOf(-1, entityCounter.toByte()),
      shortsField = setOf(-1, entityCounter.toShort()),
      intsField = setOf(-1, entityCounter),
      longsField = setOf(-1, entityCounter.toLong()),
      charsField = setOf('A', 'B'),
      floatsField = setOf(-1f, entityCounter.toFloat()),
      doublesField = setOf(-1.0, entityCounter.toDouble()),
      textListField = listOf("text $entityCounter", "text $entityCounter", "text $entityCounter"),
      numListField = listOf(entityCounter.toDouble(), entityCounter.toDouble(), 123.0),
      boolListField = listOf(true, false, true),
      instantsField = setOf(ArcsInstant.ofEpochMilli(1), ArcsInstant.ofEpochMilli(2)),
      bigintsField = setOf(BigInt.ONE, BigInt.TEN),
      inlineEntityField = getInnerEntity(),
      inlineListField = listOf(getInnerEntity(), getInnerEntity()),
      // TODO(b/174426876): add more than one entity. Currently does not work due to b/174426876.
      inlinesField = setOf(getInnerEntity()),
      referenceField = createReference("ref-$entityCounter"),
      referencesField = setOf(
        createReference("refs-$entityCounter"),
        createReference("refs-${entityCounter + 1}")
      ),
      referenceListField = listOf(
        createReference("lref-$entityCounter"),
        createReference("lrefs-${entityCounter + 1}")
      )
    )
  }

  fun generateEmpty() = FixtureEntity()

  private fun getInnerEntity(): InnerEntity {
    innerEntityCounter++
    return InnerEntity(
      textField = "inline text $innerEntityCounter",
      longField = innerEntityCounter.toLong(),
      numberField = innerEntityCounter.toDouble(),
      moreInlineField = getMoreInline(),
      moreInlinesField = setOf(getMoreInline())
    )
  }

  private fun getMoreInline() = MoreInline(textsField = setOf("more inline ${moreInlineCounter++}"))

  private fun createReference(id: String): Reference<InnerEntity> {
    return Reference(
      InnerEntity,
      arcs.core.storage.Reference(id, DummyStorageKey(id), null)
    )
  }
}
