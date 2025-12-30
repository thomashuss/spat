package io.github.thomashuss.spat.library;

class LabelKV
        extends SimpleResourceKV<Label>
{
    LabelKV(Library library)
    {
        super(library, Label.class, "label", true);
    }
}
