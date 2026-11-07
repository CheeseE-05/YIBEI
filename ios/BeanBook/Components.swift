import Foundation
import SwiftUI
import UIKit

struct Overline: View {
    let text: String
    var color: Color = C.muted

    var body: some View {
        Text(text.uppercased())
            .font(.mono(10, weight: .medium))
            .tracking(1.1)
            .foregroundStyle(color)
            .lineLimit(1)
    }
}

struct RoundIconButton: View {
    let systemName: String
    let accessibilityLabel: String
    var background: Color = C.card
    var foreground: Color = C.ink
    var size: CGFloat = 44
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(foreground)
                .frame(width: size, height: size)
                .background(background)
                .clipShape(Circle())
                .overlay(Circle().stroke(C.lineSoft, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibilityLabel)
    }
}

struct PrimaryActionButton: View {
    let title: String
    var icon: String? = nil
    var enabled: Bool = true
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let icon {
                    Image(systemName: icon).font(.system(size: 14, weight: .semibold))
                }
                Text(title).font(.system(size: 15, weight: .semibold))
            }
            .foregroundStyle(enabled ? C.onAccent : C.muted)
            .frame(maxWidth: .infinity)
            .frame(height: 51)
            .background(enabled ? C.lime : C.lineSoft)
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

struct OutlineActionButton: View {
    let title: String
    var enabled: Bool = true
    var height: CGFloat = 49
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(enabled ? C.ink : C.muted)
                .frame(maxWidth: .infinity)
                .frame(height: height)
                .background(.clear)
                .clipShape(Capsule())
                .overlay(Capsule().stroke(C.line, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

struct SelectableChip: View {
    let title: String
    let selected: Bool
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(selected ? C.card : C.inkSoft)
                .padding(.horizontal, 14)
                .frame(height: 36)
                .background(selected ? C.ink : C.card)
                .clipShape(Capsule())
                .overlay {
                    if !selected {
                        Capsule().stroke(C.lineSoft, lineWidth: 1)
                    }
                }
        }
        .buttonStyle(.plain)
    }
}

struct SmallChoiceButton: View {
    let title: String
    let selected: Bool
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 12, weight: selected ? .medium : .regular))
                .foregroundStyle(selected ? C.forest : C.inkSoft)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(selected ? C.limeSoft : C.paperSoft)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay {
                    if selected {
                        RoundedRectangle(cornerRadius: 12).stroke(C.limeDeep.opacity(0.35), lineWidth: 1)
                    }
                }
        }
        .buttonStyle(.plain)
    }
}

struct TagPill: View {
    let title: String
    var background: Color = C.paperSoft
    var foreground: Color = C.inkSoft
    var dot: Color? = nil

    var body: some View {
        HStack(spacing: 5) {
            if let dot {
                Circle().fill(dot).frame(width: 5, height: 5)
            }
            Text(title).font(.system(size: 11.5, weight: .medium))
        }
        .foregroundStyle(foreground)
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(background)
        .clipShape(Capsule())
    }
}

struct StatusPill: View {
    let status: BeanStatus

    var body: some View {
        HStack(spacing: 5) {
            Circle()
                .fill(Color(hex: status.inkHex).opacity(0.75))
                .frame(width: 4, height: 4)
            Text(status.label)
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(Color(hex: status.inkHex))
        }
        .padding(.horizontal, 9)
        .padding(.vertical, 5)
        .background(Color(hex: status.bgHex))
        .clipShape(Capsule())
    }
}

struct SectionHeading: View {
    let title: String
    var overline: String? = nil
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        HStack(alignment: .bottom) {
            VStack(alignment: .leading, spacing: 4) {
                if let overline { Overline(text: overline) }
                Text(title)
                    .font(.system(size: 21, weight: .semibold))
                    .foregroundStyle(C.ink)
            }
            Spacer()
            if let actionTitle, let action {
                Button(action: action) {
                    HStack(spacing: 2) {
                        Text(actionTitle).font(.system(size: 12.5, weight: .medium))
                        Image(systemName: "chevron.right").font(.system(size: 10, weight: .semibold))
                    }
                    .foregroundStyle(C.muted)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

struct BeanArtwork: View {
    @EnvironmentObject private var store: AppStore
    let bean: Bean
    var showStatus: Bool = true
    var corner: CGFloat = 24

    var body: some View {
        GeometryReader { geo in
            ZStack {
                Color(hex: bean.status.bgHex)
                Circle()
                    .fill(Color.white.opacity(0.32))
                    .frame(width: geo.size.width * 0.68, height: geo.size.width * 0.68)
                    .position(x: geo.size.width * 0.52, y: geo.size.height * 0.53)
                Circle()
                    .fill(Color.white.opacity(0.20))
                    .frame(width: geo.size.width * 0.3, height: geo.size.width * 0.3)
                    .position(x: geo.size.width * 0.24, y: geo.size.height * 0.25)

                if let image = store.uiImage(for: bean) {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                        .padding(geo.size.width * 0.06)
                } else {
                    BeanPlaceholder(bean: bean)
                        .padding(12)
                }

                if showStatus {
                    VStack {
                        HStack {
                            StatusPill(status: bean.status)
                            Spacer()
                        }
                        Spacer()
                        HStack {
                            Spacer()
                            Text("第\(bean.ageDays)天")
                                .font(.system(size: 9.5, weight: .medium))
                                .foregroundStyle(C.inkSoft)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(C.card.opacity(0.84))
                                .clipShape(Capsule())
                        }
                    }
                    .padding(10)
                }
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: corner, style: .continuous))
    }
}

struct BeanPlaceholder: View {
    let bean: Bean

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            ZStack {
                Ellipse()
                    .fill(Color(hex: bean.roast.toneHex))
                    .frame(width: w * 0.55, height: h * 0.65)
                    .rotationEffect(.degrees(-24))
                    .shadow(color: C.ink.opacity(0.1), radius: 3, x: 3, y: 4)
                BeanCrease()
                    .stroke(C.ink.opacity(0.5), style: StrokeStyle(lineWidth: 1.6, lineCap: .round))
                    .frame(width: w * 0.36, height: h * 0.48)
                    .rotationEffect(.degrees(-24))
                Circle()
                    .fill(Color.white.opacity(0.62))
                    .frame(width: 5, height: 5)
                    .offset(x: -w * 0.15, y: -h * 0.13)
            }
            .frame(width: w, height: h)
        }
    }
}

struct BeanCrease: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.minX + rect.width * 0.38, y: rect.minY))
        path.addCurve(
            to: CGPoint(x: rect.minX + rect.width * 0.50, y: rect.maxY),
            control1: CGPoint(x: rect.maxX, y: rect.minY + rect.height * 0.36),
            control2: CGPoint(x: rect.minX, y: rect.minY + rect.height * 0.68)
        )
        return path
    }
}

struct BeanRating: View {
    let rating: Int
    var filled: Color = C.ink
    var size: CGFloat = 15

    var body: some View {
        HStack(spacing: 2) {
            ForEach(1...5, id: \.self) { index in
                ZStack {
                    Ellipse()
                        .fill(index <= rating ? filled : C.line)
                        .frame(width: size * 0.68, height: size)
                        .rotationEffect(.degrees(-24))
                    if index <= rating {
                        Capsule()
                            .fill(Color.white.opacity(0.7))
                            .frame(width: 1.2, height: size * 0.55)
                            .rotationEffect(.degrees(-10))
                    }
                }
                .frame(width: size, height: size)
            }
        }
    }
}

struct CupCanvasView: View {
    let layers: [RecipeLayer]
    let mixed: Bool
    var normalized: Bool = false

    var body: some View {
        Canvas { context, size in
            drawCup(context: &context, size: size)
        }
    }

    private func drawCup(context: inout GraphicsContext, size: CGSize) {
        let w = size.width
        let h = size.height
        guard w > 0, h > 0 else { return }
        let topY = h * 0.075
        let bottomY = h * 0.94
        let topHalf = w * 0.345
        let bottomHalf = w * 0.265
        let cx = w / 2
        let stroke = max(1.2, w * 0.010)

        var outer = Path()
        outer.move(to: CGPoint(x: cx - topHalf, y: topY))
        outer.addLine(to: CGPoint(x: cx + topHalf, y: topY))
        outer.addLine(to: CGPoint(x: cx + bottomHalf, y: bottomY - h * 0.055))
        outer.addCurve(
            to: CGPoint(x: cx + bottomHalf - w * 0.095, y: bottomY),
            control1: CGPoint(x: cx + bottomHalf, y: bottomY - h * 0.012),
            control2: CGPoint(x: cx + bottomHalf - w * 0.045, y: bottomY)
        )
        outer.addLine(to: CGPoint(x: cx - bottomHalf + w * 0.095, y: bottomY))
        outer.addCurve(
            to: CGPoint(x: cx - bottomHalf, y: bottomY - h * 0.055),
            control1: CGPoint(x: cx - bottomHalf + w * 0.045, y: bottomY),
            control2: CGPoint(x: cx - bottomHalf, y: bottomY - h * 0.012)
        )
        outer.closeSubpath()

        let inset = max(2.4, w * 0.018)
        var inner = Path()
        inner.move(to: CGPoint(x: cx - topHalf + inset, y: topY + inset))
        inner.addLine(to: CGPoint(x: cx + topHalf - inset, y: topY + inset))
        inner.addLine(to: CGPoint(x: cx + bottomHalf - inset, y: bottomY - h * 0.058))
        inner.addCurve(
            to: CGPoint(x: cx + bottomHalf - w * 0.10, y: bottomY - inset),
            control1: CGPoint(x: cx + bottomHalf - inset, y: bottomY - h * 0.022),
            control2: CGPoint(x: cx + bottomHalf - w * 0.055, y: bottomY - inset)
        )
        inner.addLine(to: CGPoint(x: cx - bottomHalf + w * 0.10, y: bottomY - inset))
        inner.addCurve(
            to: CGPoint(x: cx - bottomHalf + inset, y: bottomY - h * 0.058),
            control1: CGPoint(x: cx - bottomHalf + w * 0.055, y: bottomY - inset),
            control2: CGPoint(x: cx - bottomHalf + inset, y: bottomY - h * 0.022)
        )
        inner.closeSubpath()

        context.fill(
            Path(ellipseIn: CGRect(x: cx - w * 0.29, y: bottomY + h * 0.006, width: w * 0.58, height: h * 0.038)),
            with: .color(C.ink.opacity(0.08))
        )
        context.fill(
            outer,
            with: .linearGradient(
                Gradient(colors: [Color.white.opacity(0.55), Color.white.opacity(0.2)]),
                startPoint: CGPoint(x: cx, y: topY),
                endPoint: CGPoint(x: cx, y: bottomY)
            )
        )

        context.drawLayer { layerContext in
            layerContext.clip(to: inner)
            layerContext.fill(
                Path(CGRect(x: cx - topHalf, y: topY, width: topHalf * 2, height: bottomY - topY)),
                with: .linearGradient(
                    Gradient(colors: [Color.white.opacity(0.38), Color.white.opacity(0.1)]),
                    startPoint: CGPoint(x: cx, y: topY),
                    endPoint: CGPoint(x: cx, y: bottomY)
                )
            )
            if !layers.isEmpty {
                let total = max(layers.reduce(0) { $0 + $1.grams }, 1)
                let denominator = normalized ? Double(total) : Double(CUP_CAPACITY)
                let fillRatio = CGFloat(min(1.0, Double(total) / denominator))
                let fillHeight = (bottomY - topY - h * 0.065) * fillRatio
                let fillBottom = bottomY - h * 0.012
                var y = fillBottom

                if mixed {
                    let color = mixedColor()
                    let rect = CGRect(x: 0, y: fillBottom - fillHeight, width: w, height: fillHeight)
                    layerContext.fill(
                        Path(rect),
                        with: .linearGradient(
                            Gradient(colors: [color.opacity(0.9), color.opacity(0.75)]),
                            startPoint: CGPoint(x: 0, y: rect.minY),
                            endPoint: CGPoint(x: 0, y: rect.maxY)
                        )
                    )
                } else {
                    for layer in layers {
                        let share = Double(layer.grams) / Double(total)
                        let layerHeight = fillHeight * CGFloat(share)
                        let rect = CGRect(x: 0, y: y - layerHeight, width: w, height: layerHeight)
                        let color = Color(hex: layer.colorHex)
                        layerContext.fill(Path(rect), with: .color(color.opacity(layer.alpha)))
                        var line = Path()
                        line.move(to: CGPoint(x: 0, y: y - layerHeight))
                        line.addLine(to: CGPoint(x: w, y: y - layerHeight))
                        layerContext.stroke(line, with: .color(Color.white.opacity(0.22)), lineWidth: 1)
                        drawTexture(layerContext: &layerContext, layer: layer, rect: rect, w: w)
                        y -= layerHeight
                    }
                }
            }
        }

        context.fill(
            Path(ellipseIn: CGRect(x: cx - topHalf, y: topY - h * 0.010, width: topHalf * 2, height: h * 0.048)),
            with: .color(Color.white.opacity(0.82))
        )
        let rim = Path(ellipseIn: CGRect(x: cx - topHalf, y: topY - h * 0.010, width: topHalf * 2, height: h * 0.048))
        context.stroke(rim, with: .color(C.ink.opacity(0.32)), lineWidth: stroke)
        context.stroke(outer, with: .color(Color.white.opacity(0.75)), lineWidth: stroke * 2.1)
        context.stroke(outer, with: .color(C.ink.opacity(0.56)), lineWidth: stroke)
        context.stroke(inner, with: .color(C.ink.opacity(0.10)), lineWidth: stroke * 0.7)

        var highlight = Path()
        highlight.move(to: CGPoint(x: cx - topHalf + w * 0.065, y: topY + h * 0.065))
        highlight.addCurve(
            to: CGPoint(x: cx - bottomHalf + w * 0.11, y: bottomY - h * 0.10),
            control1: CGPoint(x: cx - topHalf + w * 0.025, y: topY + h * 0.26),
            control2: CGPoint(x: cx - bottomHalf + w * 0.06, y: bottomY - h * 0.28)
        )
        context.stroke(
            highlight,
            with: .color(Color.white.opacity(0.48)),
            style: StrokeStyle(lineWidth: max(1.4, w * 0.012), lineCap: .round)
        )

        if layers.isEmpty {
            context.stroke(
                inner,
                with: .color(C.muted.opacity(0.4)),
                style: StrokeStyle(lineWidth: stroke * 0.8, dash: [5, 5])
            )
        }
    }

    private func drawTexture(
        layerContext: inout GraphicsContext,
        layer: RecipeLayer,
        rect: CGRect,
        w: CGFloat
    ) {
        let color = Color(hex: layer.colorHex)
        switch layer.kind {
        case .ice:
            let cube = min(rect.height * 0.45, w * 0.06)
            layerContext.fill(
                Path(roundedRect: CGRect(x: rect.midX - cube, y: rect.midY - cube * 0.5, width: cube, height: cube), cornerRadius: cube * 0.2),
                with: .color(Color.white.opacity(0.22))
            )
        case .sparkling:
            for index in 0..<5 {
                let x = rect.minX + rect.width * CGFloat(0.3 + Double(index) * 0.08)
                let y = rect.midY + rect.height * (index % 2 == 0 ? CGFloat(0.05) : CGFloat(-0.05))
                layerContext.fill(
                    Path(ellipseIn: CGRect(x: x, y: y, width: 2.4, height: 2.4)),
                    with: .color(Color.white.opacity(0.4))
                )
            }
        case .foam, .syrup:
            for index in 0..<3 {
                let x = rect.minX + rect.width * CGFloat(0.34 + Double(index) * 0.10)
                layerContext.fill(
                    Path(ellipseIn: CGRect(x: x, y: rect.midY - 2, width: 7, height: 4)),
                    with: .color(Color.white.opacity(0.18))
                )
            }
        case .chunk, .puree:
            for index in 0..<3 {
                let x = rect.minX + rect.width * CGFloat(0.32 + Double(index) * 0.1)
                layerContext.fill(
                    Path(ellipseIn: CGRect(x: x, y: rect.midY - 3, width: 6, height: 5)),
                    with: .color(color.opacity(0.65))
                )
            }
        case .liquid:
            break
        }
    }

    private func mixedColor() -> Color {
        Color.blend(layers.map {
            (Color(hex: $0.colorHex), Double($0.grams) * (0.55 + 0.45 * $0.alpha))
        })
    }
}

struct MiniCupView: View {
    let layers: [RecipeLayer]
    var mixed: Bool = false

    var body: some View {
        CupCanvasView(layers: layers, mixed: mixed, normalized: true)
    }
}

struct EmptyState: View {
    let title: String
    let message: String
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 12) {
            ZStack {
                Circle().fill(C.paperSoft).frame(width: 74, height: 74)
                Image(systemName: "leaf.fill")
                    .font(.system(size: 27, weight: .regular))
                    .foregroundStyle(C.muted)
                    .rotationEffect(.degrees(-24))
            }
            Text(title)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(C.ink)
                .multilineTextAlignment(.center)
            Text(message)
                .font(.system(size: 13))
                .foregroundStyle(C.muted)
                .multilineTextAlignment(.center)
                .lineSpacing(3)
                .fixedSize(horizontal: false, vertical: true)
            if let actionTitle, let action {
                Button(action: action) {
                    Text(actionTitle)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(C.ink)
                        .padding(.horizontal, 22)
                        .frame(height: 44)
                        .background(C.lime)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .padding(.top, 4)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 28)
        .padding(.horizontal, 20)
    }
}

struct FieldShell<Content: View>: View {
    let label: String
    let content: Content

    init(label: String, @ViewBuilder content: () -> Content) {
        self.label = label
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label.uppercased())
                .font(.mono(9, weight: .regular))
                .tracking(0.7)
                .foregroundStyle(C.muted)
            content
        }
        .padding(.horizontal, 15)
        .padding(.vertical, 11)
        .background(C.paperSoft)
        .clipShape(RoundedRectangle(cornerRadius: 17, style: .continuous))
    }
}

struct BrewTextField: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    var keyboard: UIKeyboardType = .default
    var limit: Int = 120

    var body: some View {
        FieldShell(label: label) {
            TextField(placeholder, text: Binding(
                get: { text },
                set: { text = String($0.prefix(limit)) }
            ))
            .font(.system(size: 15))
            .foregroundStyle(C.ink)
            .keyboardType(keyboard)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
        }
    }
}

struct BrewTextEditor: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    var height: CGFloat = 78

    var body: some View {
        FieldShell(label: label) {
            ZStack(alignment: .topLeading) {
                if text.isEmpty {
                    Text(placeholder)
                        .font(.system(size: 14))
                        .foregroundStyle(C.mutedLight)
                        .padding(.top, 4)
                }
                TextEditor(text: $text)
                    .font(.system(size: 14))
                    .foregroundStyle(C.ink)
                    .scrollContentBackground(.hidden)
                    .frame(height: height, alignment: .topLeading)
            }
        }
    }
}

struct MetaDot: View {
    var color: Color = C.mutedLight
    var body: some View { Circle().fill(color).frame(width: 3, height: 3) }
}

struct RecipeStat: View {
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.mono(17, weight: .medium))
                .foregroundStyle(C.ink)
            Text(label)
                .font(.mono(9))
                .foregroundStyle(C.muted)
        }
        .frame(maxWidth: .infinity)
    }
}
