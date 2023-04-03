data "aws_iam_policy_document" "this" {
  statement {
    actions = [
      "s3:*",
    ]
    resources = [
      "arn:aws:s3:::${module.s3.bucket}/*"
    ]
  }

  statement {
    actions = [
      "kms:DescribeKey",
      "kms:GenerateDataKey",
      "kms:Decrypt"
    ]
    resources = [
      var.kms_key_arn
    ]
  }
}

resource "aws_iam_policy" "this" {
  name_prefix = var.git
  policy      = data.aws_iam_policy_document.this.json
  tags        = merge(local.tags, var.tags)
}