data "aws_iam_policy_document" "this" {
  statement {
    actions = [
      "s3:PutObject"
    ]
    resources = [
      "arn:aws:s3:::${module.s3.bucket}/*"
    ]
  }
}

resource "aws_iam_policy" "this" {
  name_prefix = var.git
  policy      = data.aws_iam_policy_document.this.json
  tags        = merge(local.tags, var.tags)
}