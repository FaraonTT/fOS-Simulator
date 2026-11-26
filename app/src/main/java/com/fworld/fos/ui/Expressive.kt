package com.fworld.fos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/** Фигуры Material 3 Expressive (MaterialShapes), используемые в fOS */
enum class ShapeKind {
    Circle, Pill, Oval, Square, Slanted, Arch, Gem, Pentagon, Heart,
    Cookie4, Cookie6, Cookie9, Cookie12,
    Clover4, Clover8, Flower, SoftBurst, Burst, Sunny, VerySunny, Puffy,
}

@Composable
fun shapeOf(kind: ShapeKind): Shape = when (kind) {
    ShapeKind.Circle -> MaterialShapes.Circle.toShape()
    ShapeKind.Pill -> MaterialShapes.Pill.toShape()
    ShapeKind.Oval -> MaterialShapes.Oval.toShape()
    ShapeKind.Square -> MaterialShapes.Square.toShape()
    ShapeKind.Slanted -> MaterialShapes.Slanted.toShape()
    ShapeKind.Arch -> MaterialShapes.Arch.toShape()
    ShapeKind.Gem -> MaterialShapes.Gem.toShape()
    ShapeKind.Pentagon -> MaterialShapes.Pentagon.toShape()
    ShapeKind.Heart -> MaterialShapes.Heart.toShape()
    ShapeKind.Cookie4 -> MaterialShapes.Cookie4Sided.toShape()
    ShapeKind.Cookie6 -> MaterialShapes.Cookie6Sided.toShape()
    ShapeKind.Cookie9 -> MaterialShapes.Cookie9Sided.toShape()
    ShapeKind.Cookie12 -> MaterialShapes.Cookie12Sided.toShape()
    ShapeKind.Clover4 -> MaterialShapes.Clover4Leaf.toShape()
    ShapeKind.Clover8 -> MaterialShapes.Clover8Leaf.toShape()
    ShapeKind.Flower -> MaterialShapes.Flower.toShape()
    ShapeKind.SoftBurst -> MaterialShapes.SoftBurst.toShape()
    ShapeKind.Burst -> MaterialShapes.Burst.toShape()
    ShapeKind.Sunny -> MaterialShapes.Sunny.toShape()
    ShapeKind.VerySunny -> MaterialShapes.VerySunny.toShape()
    ShapeKind.Puffy -> MaterialShapes.Puffy.toShape()
}

/** Контейнер-фигура с однотонной заливкой */
@Composable
fun ShapeBadge(
    kind: ShapeKind,
    container: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier.size(size).clip(shapeOf(kind)).background(container),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Контейнер-фигура с градиентом */
@Composable
fun GradientShape(
    kind: ShapeKind,
    colors: List<Color>,
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier.size(size).clip(shapeOf(kind)).background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
