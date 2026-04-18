import ReactQuill from 'react-quill';
import 'quill/dist/quill.snow.css'

interface RichTextEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

const modules = {
  toolbar: [
    [{ header: [1, 2, 3, false] }],
    ['bold', 'italic', 'underline', 'strike'],
    [{ list: 'ordered' }, { list: 'bullet' }],
    [{ color: [] }, { background: [] }],
    ['blockquote', 'code-block'],
    ['link'],
    ['clean'],
  ],
};

export function RichTextEditor({ value, onChange, placeholder }: RichTextEditorProps) {
  return (
    <div className="rounded-md border border-slate-300 bg-white">
      <ReactQuill
        theme="snow"
        modules={modules}
        value={value}
        onChange={onChange}
        placeholder={placeholder || 'Write content...'}
      />
    </div>
  );
}
